package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "participaciones")
public class Participacion {

    @Id
    private String id;

    @Indexed
    private String idUsuario;

    @Indexed
    private String idProyecto;

    private LocalDateTime fechaUnion;

    public Participacion() {
        this.fechaUnion = LocalDateTime.now();
    }

    public Participacion(String idUsuario, String idProyecto) {
        this();
        this.idUsuario = idUsuario;
        this.idProyecto = idProyecto;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getIdProyecto() { return idProyecto; }
    public void setIdProyecto(String idProyecto) { this.idProyecto = idProyecto; }

    public LocalDateTime getFechaUnion() { return fechaUnion; }
    public void setFechaUnion(LocalDateTime fechaUnion) { this.fechaUnion = fechaUnion; }
}
