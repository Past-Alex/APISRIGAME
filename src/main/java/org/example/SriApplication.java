package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SriApplication {
    public static void main(String[] args) {
        SpringApplication.run(SriApplication.class, args);
        System.out.println("🚀 API SRI Levantada — LISTO PARA PROBAR EN POSTMAN");
    }
}
