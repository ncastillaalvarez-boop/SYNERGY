package com.proaula.spring.synergy.Model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** Nivel de riesgo de un proyecto en un día (uno por proyecto y fecha). */
@Document(collection = "historial_riesgo")
@CompoundIndex(name = "proyecto_fecha", def = "{'idProyecto': 1, 'fecha': 1}", unique = true)
public class HistorialRiesgo {

    @Id
    private String id;
    private String idProyecto;
    private LocalDate fecha;
    private String nivel;
    private double puntaje;

    public HistorialRiesgo() {
    }

    public HistorialRiesgo(String idProyecto, LocalDate fecha, String nivel, double puntaje) {
        this.idProyecto = idProyecto;
        this.fecha = fecha;
        this.nivel = nivel;
        this.puntaje = puntaje;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getIdProyecto() { return idProyecto; }
    public void setIdProyecto(String idProyecto) { this.idProyecto = idProyecto; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public double getPuntaje() { return puntaje; }
    public void setPuntaje(double puntaje) { this.puntaje = puntaje; }
}
