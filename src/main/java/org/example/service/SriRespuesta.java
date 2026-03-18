package org.example.service;

import org.springframework.stereotype.Service;
import recepcion.ws.sri.gob.ec.RespuestaSolicitud;
import autorizacion.ws.sri.gob.ec.RespuestaComprobante;
import autorizacion.ws.sri.gob.ec.Autorizacion;

@Service
public class SriRespuesta {

    // =========================================================
    // 🧾 Verifica si el SRI aceptó el XML en recepción
    // =========================================================
    public boolean esRecepcionValida(RespuestaSolicitud response) {
        if (response == null || response.getEstado() == null) return false;
        return response.getEstado().equalsIgnoreCase("RECIBIDA");
    }

    // =========================================================
    // 🔑 Extrae clave de acceso desde el XML firmado
    // =========================================================
    public String extraerClaveAcceso(String xml) {
        try {
            int inicio = xml.indexOf("<claveAcceso>") + "<claveAcceso>".length();
            int fin = xml.indexOf("</claveAcceso>");
            return xml.substring(inicio, fin);
        } catch (Exception e) {
            return null; // si no se encuentra, retorna null
        }
    }

    // =========================================================
    // 🔍 Ver si la autorización es AUORIZADA
    // =========================================================
    public boolean esAutorizada(RespuestaComprobante autorizacion) {
        if (autorizacion == null || autorizacion.getAutorizaciones() == null) return false;

        for (Autorizacion a : autorizacion.getAutorizaciones().getAutorizacion()) {
            if ("AUTORIZADO".equalsIgnoreCase(a.getEstado())) return true;
        }
        return false;
    }
}
