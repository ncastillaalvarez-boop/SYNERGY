package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Registro de auditoría: quién hizo qué y cuándo (aprobar, rechazar o eliminar proyectos,
 * cambiar roles, activar cuentas...). Se guarda en MongoDB y NO se borra desde la interfaz.
 */
@Document(collection = "auditoria")
public class RegistroAuditoria {

    @Id
    private String id;
    private String tipo;        // CREAR, EDITAR, ELIMINAR
    private String entidad;     // PROYECTO, TAREA, USUARIO
    private String descripcion;
    @Indexed
    private String usuario;     // correo de quien hizo la acción
    @Indexed
    private LocalDateTime fecha;

    public RegistroAuditoria() {
    }

    public RegistroAuditoria(String tipo, String entidad, String descripcion, String usuario, LocalDateTime fecha) {
        this.tipo = tipo;
        this.entidad = entidad;
        this.descripcion = descripcion;
        this.usuario = usuario;
        this.fecha = fecha;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
