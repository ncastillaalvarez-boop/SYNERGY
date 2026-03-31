package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "comentarios")
public class Comentario {

    @Id
    private String id;

    private String contenido;

    private LocalDateTime fecha;

    @Indexed
    private String idProyecto;

    @Indexed
    private String idUsuario;

    @Indexed
    private String idTarea;

    public Comentario() {
        this.fecha = LocalDateTime.now();
    }

    public Comentario(String contenido, String idProyecto, String idUsuario) {
        this();
        this.contenido = contenido;
        this.idProyecto = idProyecto;
        this.idUsuario = idUsuario;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(String idProyecto) {
        this.idProyecto = idProyecto;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(String idTarea) {
        this.idTarea = idTarea;
    }
}