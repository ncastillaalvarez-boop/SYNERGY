package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Aviso para un usuario (campana de la web y app móvil): tarea asignada, entrega revisada,
 * proyecto aprobado, riesgo alto, mensaje del líder...
 */
@Document(collection = "notificaciones")
@CompoundIndex(name = "usuario_leida_fecha", def = "{'idUsuario': 1, 'leida': 1, 'fecha': -1}")
public class Notificacion {

    @Id
    private String id;
    private String idUsuario;
    /** TAREA, ENTREGA, PROYECTO, RIESGO, MENSAJE, VOLUNTARIO */
    private String tipo;
    private String titulo;
    private String mensaje;
    /** Ruta interna de la web a la que lleva el aviso (puede ser null). */
    private String enlace;
    private boolean leida;
    private LocalDateTime fecha;

    public Notificacion() {
    }

    public Notificacion(String idUsuario, String tipo, String titulo, String mensaje, String enlace) {
        this.idUsuario = idUsuario;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.enlace = enlace;
        this.leida = false;
        this.fecha = LocalDateTime.now();
    }

    /** Icono de Font Awesome según el tipo. */
    public String getIcono() {
        if (tipo == null) {
            return "fa-bell";
        }
        return switch (tipo) {
            case "TAREA" -> "fa-list-check";
            case "ENTREGA" -> "fa-file-arrow-up";
            case "PROYECTO" -> "fa-seedling";
            case "RIESGO" -> "fa-triangle-exclamation";
            case "MENSAJE" -> "fa-bullhorn";
            case "VOLUNTARIO" -> "fa-user-plus";
            default -> "fa-bell";
        };
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public String getEnlace() { return enlace; }
    public void setEnlace(String enlace) { this.enlace = enlace; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
