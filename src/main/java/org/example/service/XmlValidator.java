package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

@Slf4j
@Component
public class XmlValidator {

    private final Schema schema;

    public XmlValidator() {
        try {
            SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            Source schemaSource = new StreamSource(
                    this.getClass().getResourceAsStream("/xsd/factura.xsd"));

            this.schema = factory.newSchema(schemaSource);
        } catch (SAXException e) {
            throw new IllegalStateException("No se pudo cargar el XSD de factura", e);
        }
    }

    public void validar(String xml) {
        try {
            Validator validator = schema.newValidator();
            Source xmlSource = new StreamSource(new StringReader(xml));
            validator.validate(xmlSource);
            log.info("✅ XML válido contra factura.xsd");
        } catch (SAXException | IOException e) {
            log.error("❌ XML NO válido contra factura.xsd: {}", e.getMessage());
            throw new RuntimeException("XML no válido: " + e.getMessage(), e);
        }
    }
}
