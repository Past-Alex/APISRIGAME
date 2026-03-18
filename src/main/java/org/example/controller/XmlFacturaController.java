package org.example.controller;

import org.example.service.FirmaService;
import org.example.service.SriService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sri")
public class XmlFacturaController {

    private final FirmaService firmaService;
    private final SriService sriService;

    // ===============================
    // CONSTRUCTOR (inyección limpia)
    // ===============================
    public XmlFacturaController(
            FirmaService firmaService,
            SriService sriService
    ) {
        this.firmaService = firmaService;
        this.sriService = sriService;
    }

    // =====================================================
    // 🔥 1) FIRMAR XML
    // =====================================================
    @PostMapping("/firmar")
    public ResponseEntity<?> firmarXml(@RequestBody String xml) {
        try {
            String xmlFirmado = firmaService.firmarXML(xml);
            return ResponseEntity.ok(xmlFirmado);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("❌ Error firmando XML: " + e.getMessage());
        }
    }

    // =====================================================
    // 🔥 2) RECEPCIÓN SRI
    // =====================================================
    @PostMapping("/recepcion")
    public ResponseEntity<?> recepcion(@RequestBody String xmlFirmado) {
        try {
            String respuestaSoap =
                    sriService.enviarComprobanteRecepcion(xmlFirmado);

            // Se devuelve el SOAP crudo (igual que SoapUI)
            return ResponseEntity.ok(respuestaSoap);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("❌ Error en recepción SRI: " + e.getMessage());
        }
    }

    // =====================================================
    // 🔥 3) AUTORIZACIÓN SRI
    // =====================================================
    @GetMapping("/autorizacion/{claveAcceso}")
    public ResponseEntity<?> autorizacion(
            @PathVariable String claveAcceso
    ) {
        try {
            String respuestaSoap =
                    sriService.autorizarComprobante(claveAcceso);

            return ResponseEntity.ok(respuestaSoap);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("❌ Error en autorización SRI: " + e.getMessage());
        }
    }
}
