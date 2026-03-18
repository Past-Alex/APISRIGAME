package org.example.service;

import jakarta.xml.soap.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service.Mode;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SriService {

    // ===============================
    // WSDLs (Recepción / Autorización)
    // ===============================
    @Value("${sri.recepcion.wsdl}")
    private String recepcionWsdl;

    @Value("${sri.autorizacion.wsdl}")
    private String autorizacionWsdl;

    // ===============================
    // CONSTANTES SRI
    // ===============================
    private static final String RECEPCION_NS = "http://ec.gob.sri.ws.recepcion";
    private static final String AUTORIZACION_NS = "http://ec.gob.sri.ws.autorizacion";

    private static final String RECEPCION_SERVICE = "RecepcionComprobantesOfflineService";
    private static final String RECEPCION_PORT = "RecepcionComprobantesOfflinePort";

    private static final String AUTORIZACION_SERVICE = "AutorizacionComprobantesOfflineService";
    private static final String AUTORIZACION_PORT = "AutorizacionComprobantesOfflinePort";

    // =========================================================
    // 🔥 RECEPCIÓN (IGUAL A SOAP UI)
    // =========================================================
    public String enviarComprobanteRecepcion(String xmlFirmado) {

        try {
            // 1️⃣ XML → BASE64 (sin saltos)
            String base64 = Base64.getEncoder()
                    .encodeToString(xmlFirmado.getBytes(StandardCharsets.UTF_8));

            // 2️⃣ Crear servicio SOAP
            QName serviceName = new QName(RECEPCION_NS, RECEPCION_SERVICE);
            QName portName = new QName(RECEPCION_NS, RECEPCION_PORT);

            jakarta.xml.ws.Service service =
                    jakarta.xml.ws.Service.create(new URL(recepcionWsdl), serviceName);

            Dispatch<SOAPMessage> dispatch =
                    service.createDispatch(portName, SOAPMessage.class, Mode.MESSAGE);

            // 3️⃣ Construir SOAP EXACTO como SoapUI
            SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
            SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();

            SOAPElement validar = body.addChildElement(
                    "validarComprobante", "ec", RECEPCION_NS
            );

            // ⚠️ CLAVE: <xml> SIN namespace
            SOAPElement xml = validar.addChildElement("xml");
            xml.addTextNode(base64);

            soapMessage.saveChanges();

            // 4️⃣ Enviar
            SOAPMessage response = dispatch.invoke(soapMessage);

            return soapToString(response);

        } catch (Exception e) {
            throw new RuntimeException("❌ Error enviando comprobante a SRI (Recepción)", e);
        }
    }

    // =========================================================
    // 🔥 AUTORIZACIÓN (IGUAL A SOAP UI)
    // =========================================================
    public String autorizarComprobante(String claveAcceso) {

        try {
            QName serviceName = new QName(AUTORIZACION_NS, AUTORIZACION_SERVICE);
            QName portName = new QName(AUTORIZACION_NS, AUTORIZACION_PORT);

            jakarta.xml.ws.Service service =
                    jakarta.xml.ws.Service.create(new URL(autorizacionWsdl), serviceName);

            Dispatch<SOAPMessage> dispatch =
                    service.createDispatch(portName, SOAPMessage.class, Mode.MESSAGE);

            SOAPMessage soapMessage = MessageFactory.newInstance().createMessage();
            SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
            SOAPBody body = envelope.getBody();

            SOAPElement autorizacion = body.addChildElement(
                    "autorizacionComprobante", "ec", AUTORIZACION_NS
            );

            SOAPElement clave = autorizacion.addChildElement("claveAccesoComprobante");
            clave.addTextNode(claveAcceso);

            soapMessage.saveChanges();

            SOAPMessage response = dispatch.invoke(soapMessage);

            return soapToString(response);

        } catch (Exception e) {
            throw new RuntimeException("❌ Error consultando autorización SRI", e);
        }
    }

    // =========================================================
    // 🧰 UTIL: SOAP → STRING
    // =========================================================
    private String soapToString(SOAPMessage msg) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        msg.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }
}
