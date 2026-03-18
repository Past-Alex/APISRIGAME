package org.example.dto;

import java.util.List;

public class RecepcionResponseDTO {

    private String estado;        // RECIBIDA / DEVUELTA
    private String claveAcceso;   // extraída del XML
    private List<MensajeDTO> mensajes;

    // ===== getters & setters =====

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getClaveAcceso() {
        return claveAcceso;
    }

    public void setClaveAcceso(String claveAcceso) {
        this.claveAcceso = claveAcceso;
    }

    public List<MensajeDTO> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<MensajeDTO> mensajes) {
        this.mensajes = mensajes;
    }
}
