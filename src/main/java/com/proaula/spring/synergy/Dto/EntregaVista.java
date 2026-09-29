package com.proaula.spring.synergy.Dto;

import java.time.LocalDateTime;

/**
 * Datos de una entrega de tarea listos para mostrar en la vista del líder
 * (Lider_Tareas_Recibidas.html).
 */
public class EntregaVista {

    private final String id;
    private final String titulo;
    private final String descripcion;
    private final String proyecto;
    private final String nombreUsuario;
    private final String inicialesUsuario;
    private final String nombreArchivo;
    private final LocalDateTime fechaEntrega;
    private final String estado;

    public EntregaVista(String id, String titulo, String descripcion, String proyecto,
                        String nombreUsuario, String inicialesUsuario, String nombreArchivo,
                        LocalDateTime fechaEntrega, String estado) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.proyecto = proyecto;
        this.nombreUsuario = nombreUsuario;
        this.inicialesUsuario = inicialesUsuario;
        this.nombreArchivo = nombreArchivo;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getProyecto() { return proyecto; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getInicialesUsuario() { return inicialesUsuario; }
    public String getNombreArchivo() { return nombreArchivo; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public String getEstado() { return estado; }
}
