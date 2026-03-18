package org.example.dto;

public class MensajeDTO {

    private String identificador;
    private String mensaje;
    private String tipo;     // ERROR / ADVERTENCIA / INFORMACION

    // ===== getters & setters =====

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
