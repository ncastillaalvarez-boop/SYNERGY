package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "privilegios")
public class Privilegio {

    @Id
    private String id;

    @Indexed(unique = true)
    private String nombre; // EJ: CREAR_PROYECTO, EDITAR_USUARIO, etc.

    private String descripcion;

    @Indexed
    private String recurso; // PROYECTO, TAREA, USUARIO, COMENTARIO, ENTREGA, ADMIN

    @Indexed
    private String accion; // CREATE, READ, UPDATE, DELETE

    @Indexed
    private Boolean activo;

    private LocalDateTime fechaCreacion;

    public Privilegio() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Privilegio(String nombre, String descripcion, String recurso, String accion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.recurso = recurso;
        this.accion = accion;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
