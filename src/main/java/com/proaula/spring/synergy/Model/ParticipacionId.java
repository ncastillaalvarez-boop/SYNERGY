package com.proaula.spring.synergy.Model;

import java.io.Serializable;

public class ParticipacionId implements Serializable {

    private String usuarioId;
    private String proyectoId;

    public ParticipacionId() {}

    public ParticipacionId(String usuarioId, String proyectoId) {
        this.usuarioId = usuarioId;
        this.proyectoId = proyectoId;
    }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }

    public String getProyectoId() { return proyectoId; }
    public void setProyectoId(String proyectoId) { this.proyectoId = proyectoId; }

    // equals y hashCode SON OBLIGATORIOS
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParticipacionId)) return false;
        ParticipacionId that = (ParticipacionId) o;
        return usuarioId.equals(that.usuarioId) && proyectoId.equals(that.proyectoId);
    }

    @Override
    public int hashCode() {
        return usuarioId.hashCode() + proyectoId.hashCode();
    }
}
