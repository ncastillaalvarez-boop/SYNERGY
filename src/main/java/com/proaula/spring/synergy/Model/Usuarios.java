package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Document(collection = "usuarios")
public class Usuarios implements UserDetails {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String correo;

    private String contrasena;

    @Indexed
    private String idRol;

    @Transient
    private String rolNombre;

    @Indexed
    private String estado;

    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime ultimoCambioContrasena;
    private LocalDateTime fechaActualizacion;

    public Usuarios() {
        this.estado = "ACTIVO";
        this.fechaCreacion = LocalDateTime.now();
        this.ultimoCambioContrasena = LocalDateTime.now();
    }

    public Usuarios(String nombre, String correo, String contrasena) {
        this();
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (rolNombre != null && !rolNombre.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rolNombre));
        }
        return authorities;
    }

    @Override
    public String getPassword() { return this.contrasena; }

    @Override
    public String getUsername() { return this.correo; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return !"INACTIVO".equals(this.estado); }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return "ACTIVO".equals(this.estado); }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
        this.ultimoCambioContrasena = LocalDateTime.now();
    }

    public String getIdRol() { return idRol; }
    public void setIdRol(String idRol) { this.idRol = idRol; }

    // getRol/setRol operan sobre el campo transient (no persistido)
    // para mantener compatibilidad con Spring Security y controladores
    public String getRol() { return rolNombre; }
    public void setRol(String rolNombre) { this.rolNombre = rolNombre; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }

    public LocalDateTime getUltimoCambioContrasena() { return ultimoCambioContrasena; }
    public void setUltimoCambioContrasena(LocalDateTime ultimoCambioContrasena) {
        this.ultimoCambioContrasena = ultimoCambioContrasena;
    }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
