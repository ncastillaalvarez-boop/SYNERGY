package com.proaula.spring.synergy.Dto;

import java.time.LocalDateTime;

/**
 * Comentario con el nombre del autor, listo para Comentarios_Proyectos.html.
 */
public class ComentarioVista {

    private final String id;
    private final String autor;
    private final String iniciales;
    private final String contenido;
    private final LocalDateTime fecha;
    private final boolean propio;

    public ComentarioVista(String id, String autor, String iniciales, String contenido,
                           LocalDateTime fecha, boolean propio) {
        this.id = id;
        this.autor = autor;
        this.iniciales = iniciales;
        this.contenido = contenido;
        this.fecha = fecha;
        this.propio = propio;
    }

    public String getId() { return id; }
    public String getAutor() { return autor; }
    public String getIniciales() { return iniciales; }
    public String getContenido() { return contenido; }
    public LocalDateTime getFecha() { return fecha; }
    public boolean isPropio() { return propio; }
}
