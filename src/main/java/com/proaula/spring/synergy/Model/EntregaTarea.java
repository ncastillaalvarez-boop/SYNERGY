package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "entregas_tareas")
public class EntregaTarea {

    @Id
    private String id;

    @Indexed
    private String idTarea;

    @Indexed
    private String idUsuario;

    private String nombreArchivo;

    private String tipoArchivo;

    private byte[] datos;

    @Indexed
    private LocalDateTime fechaEntrega;

    private String descripcionEvidencia;

    private String estado; // ENTREGADO, REVISADO, CALIFICADO, RECHAZADO
    private String calificacion;
    private String comentariosRevisor;

    public EntregaTarea() {
        this.fechaEntrega = LocalDateTime.now();
        this.estado = "ENTREGADO";
    }

    public EntregaTarea(String idTarea, String idUsuario, String nombreArchivo, 
                        String tipoArchivo, byte[] datos) {
        this();
        this.idTarea = idTarea;
        this.idUsuario = idUsuario;
        this.nombreArchivo = nombreArchivo;
        this.tipoArchivo = tipoArchivo;
        this.datos = datos;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(String idTarea) {
        this.idTarea = idTarea;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoArchivo() {
        return tipoArchivo;
    }

    public void setTipoArchivo(String tipoArchivo) {
        this.tipoArchivo = tipoArchivo;
    }

    public byte[] getDatos() {
        return datos;
    }

    public void setDatos(byte[] datos) {
        this.datos = datos;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(String calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentariosRevisor() {
        return comentariosRevisor;
    }

    public void setComentariosRevisor(String comentariosRevisor) {
        this.comentariosRevisor = comentariosRevisor;
    }

    public String getDescripcionEvidencia() {
        return descripcionEvidencia;
    }

    public void setDescripcionEvidencia(String descripcionEvidencia) {
        this.descripcionEvidencia = descripcionEvidencia;
    }
}
