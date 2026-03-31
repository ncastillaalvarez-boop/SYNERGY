package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "roles")
public class Rol {

    @Id
    private String id;

    @Indexed(unique = true)
    private String nombre; // ADMINISTRADOR, LIDER, PARTICIPANTE

    private String descripcion;

    private List<String> privilegios = new ArrayList<>();

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    public Rol() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
    }

    public Rol(String nombre, String descripcion) {
        this();
        this.nombre = nombre;
        this.descripcion = descripcion;
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

    public List<String> getPrivilegios() {
        return privilegios;
    }

    public void setPrivilegios(List<String> privilegios) {
        this.privilegios = privilegios;
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

    // Métodos de utilidad
    public void addPrivilegio(String privilegioId) {
        if (!this.privilegios.contains(privilegioId)) {
            this.privilegios.add(privilegioId);
        }
    }

    public void removePrivilegio(String privilegioId) {
        this.privilegios.remove(privilegioId);
    }
}
