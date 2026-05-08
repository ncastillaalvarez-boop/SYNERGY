package com.proaula.spring.synergy.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "proyectos")
public class Proyecto {

    @Id
    private String id;

    private String nombre;

    private String descripcion;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @Indexed
    private String idLider;

    @Indexed
    private List<String> participantes = new ArrayList<>();

    @Indexed
    private String estado; // ACTIVO, COMPLETADO, CANCELADO

    private List<String> comentarios = new ArrayList<>();
    private List<String> tareas = new ArrayList<>();

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private byte[] datos;
    private String nombreArchivo;
    private String tipoArchivo;

    public Proyecto() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = "ACTIVO";
    }

    public Proyecto(String nombre, String descripcion, LocalDate fechaInicio, 
                    LocalDate fechaFin, String idLider) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.idLider = idLider;
        this.participantes.add(idLider);
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getIdLider() {
        return idLider;
    }

    public void setIdLider(String idLider) {
        this.idLider = idLider;
    }

    public List<String> getParticipantes() {
        return participantes;
    }

    public void setParticipantes(List<String> participantes) {
        this.participantes = participantes;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<String> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<String> comentarios) {
        this.comentarios = comentarios;
    }

    public List<String> getTareas() {
        return tareas;
    }

    public void setTareas(List<String> tareas) {
        this.tareas = tareas;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public byte[] getDatos() { return datos; }
    public void setDatos(byte[] datos) { this.datos = datos; }

    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }

    public String getTipoArchivo() { return tipoArchivo; }
    public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }

    // Métodos de utilidad
    public void addParticipante(String usuarioId) {
        if (!this.participantes.contains(usuarioId)) {
            this.participantes.add(usuarioId);
        }
    }

    public void removeParticipante(String usuarioId) {
        this.participantes.remove(usuarioId);
    }

    public void addTarea(String tareaId) {
        if (!this.tareas.contains(tareaId)) {
            this.tareas.add(tareaId);
        }
    }

    public void addComentario(String comentarioId) {
        if (!this.comentarios.contains(comentarioId)) {
            this.comentarios.add(comentarioId);
        }
    }
}