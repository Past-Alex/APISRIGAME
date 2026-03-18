package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class FirmaService {

    @Value("${SRI_CERTIFICATE_BASE64}")
    private String certificadoBase64;

    @Value("${SRI_CERTIFICATE_PASSWORD}")
    private String passwordCert;

    private static final String XSD_PATH = "/xsd/factura.xsd";

    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String XADES_NS = "http://uri.etsi.org/01903/v1.3.2#";

    // C14N 1.0 inclusive (como SRI suele usar en ejemplos)
    private static final String C14N_INCLUSIVE = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    // SRI clásico: RSA-SHA1 / SHA1
    private static final String SIGN_ALG_RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    private static final String DIGEST_ALG_SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    // ===================== MÉTODO PRINCIPAL =====================

    public String firmarXML(String xml) {
        try {
            log.info("🔐 Iniciando firma XAdES-BES (SRI)");

            // 1) Asegurar schemaLocation (opcional pero recomendado si validas XSD)
            if (!xml.contains("xsi:noNamespaceSchemaLocation")) {
                xml = xml.replace("<factura",
                        "<factura xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                                "xsi:noNamespaceSchemaLocation=\"factura.xsd\"");
            }

            // 2) Validación XSD (si tu XSD está correcto en /resources/xsd/factura.xsd)
            validarContraXSD(xml);

            // 3) Parse a DOM
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);

            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element factura = doc.getDocumentElement();

            // 4) id="comprobante" debe existir y ser ID
            if (!factura.hasAttribute("id")) {
                factura.setAttribute("id", "comprobante");
            }
            factura.setIdAttribute("id", true);

            // 5) Asegurar xmlns:xades en el root (para que se vea como el ejemplo)
            if (!factura.hasAttribute("xmlns:xades")) {
                factura.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", XADES_NS);
            }

            // 6) Cargar certificado y private key
            X509Certificate cert = FirmaUtils.getCertificateFromBase64(certificadoBase64, passwordCert);
            PrivateKey privateKey = FirmaUtils.getPrivateKeyFromBase64(certificadoBase64, passwordCert);

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            // 7) IDs estilo SRI
            String signatureId = "xmldsig-" + UUID.randomUUID();
            String refFacturaId = signatureId + "-ref0";
            String signedPropsId = signatureId + "-signedprops";

            // ---------- Reference #1: comprobante ----------
            // NOTA: para evitar "Ambiguous method call", casteamos a TransformParameterSpec
            Transform envTransform = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);

            Reference refFactura = fac.newReference(
                    "#comprobante",
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(envTransform),
                    null,
                    refFacturaId
            );

            // ---------- Reference #2: SignedProperties (OBLIGATORIA) ----------
            // En muchos firmadores SRI, esta Reference lleva C14N como Transform.
            Transform c14nTransform = fac.newTransform(C14N_INCLUSIVE, (TransformParameterSpec) null);

            Reference refSignedProps = fac.newReference(
                    "#" + signedPropsId,
                    fac.newDigestMethod(DigestMethod.SHA1, null),
                    Collections.singletonList(c14nTransform),
                    "http://uri.etsi.org/01903#SignedProperties",
                    null
            );

            // 8) SignedInfo
            SignedInfo signedInfo = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(
                            CanonicalizationMethod.INCLUSIVE,
                            (C14NMethodParameterSpec) null
                    ),
                    fac.newSignatureMethod(SIGN_ALG_RSA_SHA1, null),
                    Arrays.asList(refFactura, refSignedProps)
            );

            // 9) KeyInfo con X509Certificate
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));
            KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(x509Data));

            // 10) Crear XAdES-BES (QualifyingProperties completo)
            Element xades = crearXAdES(doc, cert, signatureId, signedPropsId, refFacturaId);

            XMLObject xadesObject = fac.newXMLObject(
                    Collections.singletonList(new DOMStructure(xades)),
                    null, null, null
            );

            // 11) Construir firma
            XMLSignature signature = fac.newXMLSignature(
                    signedInfo,
                    keyInfo,
                    Collections.singletonList(xadesObject),
                    signatureId,
                    null
            );

            // 12) Firmar dentro del root <factura>
            DOMSignContext dsc = new DOMSignContext(privateKey, factura);
            dsc.setDefaultNamespacePrefix("ds");

            signature.sign(dsc);

            log.info("✔ XML firmado correctamente (XAdES-BES)");
            return documentToString(doc);

        } catch (Exception e) {
            log.error("❌ Error firmando XML", e);
            throw new RuntimeException("No se pudo firmar XML → " + e.getMessage(), e);
        }
    }

    // ===================== XAdES =====================

    private Element crearXAdES(
            Document doc,
            X509Certificate cert,
            String signatureId,
            String signedPropsId,
            String refFacturaId
    ) throws Exception {

        // <xades:QualifyingProperties Target="#signatureId">
        Element qp = doc.createElementNS(XADES_NS, "xades:QualifyingProperties");
        qp.setAttribute("Target", "#" + signatureId);

        // <xades:SignedProperties Id="...-signedprops">
        Element sp = doc.createElementNS(XADES_NS, "xades:SignedProperties");
        sp.setAttribute("Id", signedPropsId);

        // CRÍTICO: marcar Id como ID real para resolver Reference URI="#...-signedprops"
        sp.setIdAttribute("Id", true);

        qp.appendChild(sp);

        // <xades:SignedSignatureProperties>
        Element ssp = doc.createElementNS(XADES_NS, "xades:SignedSignatureProperties");
        sp.appendChild(ssp);

        // SigningTime (Ecuador -05:00)
        Element signingTime = doc.createElementNS(XADES_NS, "xades:SigningTime");

        OffsetDateTime nowEc = OffsetDateTime.now(ZoneOffset.ofHours(-5));
        // Formato típico: 2025-11-16T16:44:49.870-05:00
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        signingTime.setTextContent(nowEc.format(fmt));

        ssp.appendChild(signingTime);

        // SigningCertificate
        Element signingCert = doc.createElementNS(XADES_NS, "xades:SigningCertificate");
        ssp.appendChild(signingCert);

        Element certEl = doc.createElementNS(XADES_NS, "xades:Cert");
        signingCert.appendChild(certEl);

        // CertDigest (SHA1)
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] certDigestBytes = md.digest(cert.getEncoded());

        Element certDigest = doc.createElementNS(XADES_NS, "xades:CertDigest");
        certEl.appendChild(certDigest);

        Element dm = doc.createElementNS(DS_NS, "ds:DigestMethod");
        dm.setAttribute("Algorithm", DIGEST_ALG_SHA1);
        certDigest.appendChild(dm);

        Element dv = doc.createElementNS(DS_NS, "ds:DigestValue");
        dv.setTextContent(Base64.getEncoder().encodeToString(certDigestBytes));
        certDigest.appendChild(dv);

        // IssuerSerial
        Element issuerSerial = doc.createElementNS(XADES_NS, "xades:IssuerSerial");
        certEl.appendChild(issuerSerial);

        Element issuerName = doc.createElementNS(DS_NS, "ds:X509IssuerName");
        issuerName.setTextContent(cert.getIssuerX500Principal().getName());
        issuerSerial.appendChild(issuerName);

        Element serial = doc.createElementNS(DS_NS, "ds:X509SerialNumber");
        serial.setTextContent(cert.getSerialNumber().toString());
        issuerSerial.appendChild(serial);

        // SignedDataObjectProperties
        Element sdop = doc.createElementNS(XADES_NS, "xades:SignedDataObjectProperties");
        sp.appendChild(sdop);

        Element dof = doc.createElementNS(XADES_NS, "xades:DataObjectFormat");
        dof.setAttribute("ObjectReference", "#" + refFacturaId);
        sdop.appendChild(dof);

        Element desc = doc.createElementNS(XADES_NS, "xades:Description");
        desc.setTextContent("FIRMA DIGITAL SRI");
        dof.appendChild(desc);

        Element mime = doc.createElementNS(XADES_NS, "xades:MimeType");
        mime.setTextContent("text/xml");
        dof.appendChild(mime);

        Element enc = doc.createElementNS(XADES_NS, "xades:Encoding");
        enc.setTextContent("UTF-8");
        dof.appendChild(enc);

        return qp;
    }

    // ===================== VALIDACIÓN XSD =====================

    private void validarContraXSD(String xml) throws Exception {
        log.info("🔍 Validando XML contra XSD factura.xsd");

        Schema schema = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
                .newSchema(new StreamSource(getClass().getResourceAsStream(XSD_PATH)));

        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));

        log.info("✔ XML válido contra XSD");
    }

    // ===================== DOM → STRING =====================

    private String documentToString(Document doc) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        tf.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter sw = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }
}
