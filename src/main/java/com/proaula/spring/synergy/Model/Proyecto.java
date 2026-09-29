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

    /** Código único legible, p. ej. SYN-2026-00042 (lo asigna CodigoProyectoService). */
    @Indexed(unique = true, sparse = true)
    private String codigo;

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

    // Clasificación (ver Model/Catalogos): se usa en los filtros de búsqueda
    @Indexed
    private String categoria;

    @Indexed
    private String tipoOrganizacion;

    /** Barrio o sector de Cartagena donde se realiza el proyecto. */
    @Indexed
    private String ubicacion;

    /** Público al que va dirigido. */
    private String audiencia;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

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

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getTipoOrganizacion() {
        return tipoOrganizacion;
    }

    public void setTipoOrganizacion(String tipoOrganizacion) {
        this.tipoOrganizacion = tipoOrganizacion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getAudiencia() {
        return audiencia;
    }

    public void setAudiencia(String audiencia) {
        this.audiencia = audiencia;
    }

    /** Número de integrantes sin contar al líder. */
    public int getTotalVoluntarios() {
        if (participantes == null) {
            return 0;
        }
        return (int) participantes.stream().filter(id -> id != null && !id.equals(idLider)).count();
    }

    public String getEstadoEtiqueta() {
        return Catalogos.etiquetaEstado(estado);
    }
}
