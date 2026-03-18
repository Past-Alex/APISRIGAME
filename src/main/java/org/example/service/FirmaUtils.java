package org.example.service;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

public class FirmaUtils {

    /**
     * Carga KeyStore desde un Base64 que contiene un archivo .p12
     */
    private static KeyStore loadKeyStore(String base64Pkcs12, String password) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64Pkcs12);
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new ByteArrayInputStream(decoded), password.toCharArray());
        return ks;
    }

    /**
     * Devuelve el alias correcto (el que contiene clave privada)
     */
    private static String getPrivateKeyAlias(KeyStore ks, String password) throws Exception {
        Enumeration<String> aliases = ks.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {  // 🔥 este alias tiene clave privada
                return alias;
            }
        }

        throw new Exception("No se encontró alias con clave privada en el certificado.");
    }

    /**
     * Obtiene la clave privada del .p12 decodificado
     */
    public static PrivateKey getPrivateKeyFromBase64(String base64, String password) throws Exception {
        KeyStore ks = loadKeyStore(base64, password);
        String alias = getPrivateKeyAlias(ks, password);
        return (PrivateKey) ks.getKey(alias, password.toCharArray());
    }

    /**
     * Obtiene el certificado X509 correcto (el que corresponde al alias de la clave privada)
     */
    public static X509Certificate getCertificateFromBase64(String base64, String password) throws Exception {
        KeyStore ks = loadKeyStore(base64, password);
        String alias = getPrivateKeyAlias(ks, password);
        return (X509Certificate) ks.getCertificate(alias);
    }

    /**
     * Convierte un DOM a String bien formateado
     */
    public static String xmlToString(Document doc) throws Exception {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        tf.transform(new DOMSource(doc), new StreamResult(writer));

        return writer.toString();
    }
}
