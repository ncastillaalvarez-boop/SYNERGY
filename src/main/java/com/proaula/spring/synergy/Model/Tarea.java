package com.proaula.spring.synergy.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "tareas")
public class Tarea {

    @Id
    private String id;

    private String titulo;

    private String descripcion;

    @Indexed
    private String estado; // PENDIENTE, EN_PROGRESO, COMPLETADA, CANCELADA

    private LocalDate fechaEntrega;

    @Indexed
    private String idProyecto;

    @Indexed
    private List<String> usuariosAsignados = new ArrayList<>();

    private List<String> entregas = new ArrayList<>();
    private List<String> comentarios = new ArrayList<>();

    private List<Subtarea> subtareas = new ArrayList<>();

    public static class Subtarea {
        private String id;
        private String titulo;
        private boolean completada;

        public Subtarea() { this.id = UUID.randomUUID().toString(); }
        public Subtarea(String titulo) { this(); this.titulo = titulo; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public boolean isCompletada() { return completada; }
        public void setCompletada(boolean completada) { this.completada = completada; }
    }

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public Tarea() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = "PENDIENTE";
    }

    public Tarea(String titulo, String descripcion, String estado,
                 LocalDate fechaEntrega, String idProyecto) {
        this();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.estado = estado;
        this.fechaEntrega = fechaEntrega;
        this.idProyecto = idProyecto;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDate fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getIdProyecto() {
        return idProyecto;
    }

    public void setIdProyecto(String idProyecto) {
        this.idProyecto = idProyecto;
    }

    public List<String> getUsuariosAsignados() {
        return usuariosAsignados;
    }

    public void setUsuariosAsignados(List<String> usuariosAsignados) {
        this.usuariosAsignados = usuariosAsignados;
    }

    public List<String> getEntregas() {
        return entregas;
    }

    public void setEntregas(List<String> entregas) {
        this.entregas = entregas;
    }

    public List<String> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<String> comentarios) {
        this.comentarios = comentarios;
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

    // Métodos de utilidad
    public void addUsuarioAsignado(String usuarioId) {
        if (!this.usuariosAsignados.contains(usuarioId)) {
            this.usuariosAsignados.add(usuarioId);
        }
    }

    public void removeUsuarioAsignado(String usuarioId) {
        this.usuariosAsignados.remove(usuarioId);
    }

    public void addEntrega(String entregaId) {
        if (!this.entregas.contains(entregaId)) {
            this.entregas.add(entregaId);
        }
    }

    public void addComentario(String comentarioId) {
        if (!this.comentarios.contains(comentarioId)) {
            this.comentarios.add(comentarioId);
        }
    }

    public List<Subtarea> getSubtareas() { return subtareas; }
    public void setSubtareas(List<Subtarea> subtareas) { this.subtareas = subtareas; }

    public int getSubtareasCompletadas() {
        if (subtareas == null) return 0;
        return (int) subtareas.stream().filter(Subtarea::isCompletada).count();
    }

    public int getPorcentajeSubtareas() {
        if (subtareas == null || subtareas.isEmpty()) return 0;
        return (int) Math.round(100.0 * getSubtareasCompletadas() / subtareas.size());
    }
}

