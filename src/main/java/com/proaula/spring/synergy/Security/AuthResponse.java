package com.proaula.spring.synergy.Security;

import java.util.List;

public class AuthResponse {

    private String token;
    private String tipo;
    private String id;
    private String nombre;
    private String correo;
    private String rol;
    private List<String> privilegios;
    private Long expiresIn;
    /** Token para pedir uno de acceso nuevo en POST /api/auth/refresh (dura días). */
    private String refreshToken;

    public AuthResponse() {}

    public AuthResponse(String token, String id, String nombre, String correo, 
                        String rol, List<String> privilegios, Long expiresIn) {
        this.token = token;
        this.tipo = "Bearer";
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.privilegios = privilegios;
        this.expiresIn = expiresIn;
    }

    // Getters y Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public List<String> getPrivilegios() {
        return privilegios;
    }

    public void setPrivilegios(List<String> privilegios) {
        this.privilegios = privilegios;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /** Agrega el token de renovación (devuelve this para encadenar). */
    public AuthResponse conRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
}
