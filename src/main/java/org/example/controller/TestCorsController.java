package org.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestCorsController {

    /**
     * Endpoint SOLO para probar CORS desde el frontend (Firebase / React)
     * No firma XML, no llama al SRI.
     */
    @GetMapping("/api/test/cors")
    public String testCors() {
        return "CORS OK 🚀 Backend Java responde correctamente";
    }
}
