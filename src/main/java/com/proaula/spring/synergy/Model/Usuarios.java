package com.proaula.spring.synergy.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Document(collection = "usuarios")
public class Usuarios implements UserDetails {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String correo;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String contrasena;

    @JsonIgnore
    private String salt;

    @Indexed
    private String rol; // ADMINISTRADOR, LIDER, PARTICIPANTE

    @Indexed
    private String estado; // ACTIVO, INACTIVO

    @Indexed
    private Boolean activo;

    // LOCAL (correo y contraseña) o GOOGLE (inicio de sesión con Google)
    private String proveedor = "LOCAL";

    // Foto de perfil (se llena al entrar con Google)
    private String fotoUrl;

    private List<String> privilegios = new ArrayList<>();
    private List<String> proyectos = new ArrayList<>();
    private List<String> tareasAsignadas = new ArrayList<>();

    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime ultimaCambioContrasena;

    public Usuarios() {
        this.activo = true;
        this.estado = "ACTIVO";
        this.fechaCreacion = LocalDateTime.now();
        this.ultimaCambioContrasena = LocalDateTime.now();
    }

    public Usuarios(String nombre, String correo, String contrasena, String salt, String rol) {
        this();
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.salt = salt;
        this.rol = rol;
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.rol));
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.contrasena;
    }

    @Override
    public String getUsername() {
        return this.correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(this.activo);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.activo) && "ACTIVO".equalsIgnoreCase(this.estado);
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
        this.ultimaCambioContrasena = LocalDateTime.now();
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    /** Dos letras para el avatar (ej. "Nictor Cañate" -> "NC"). */
    public String getIniciales() {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String ini = partes[0].substring(0, 1);
        if (partes.length > 1) {
            ini += partes[1].substring(0, 1);
        } else if (partes[0].length() > 1) {
            ini += partes[0].substring(1, 2);
        }
        return ini.toUpperCase();
    }

    public List<String> getPrivilegios() {
        return privilegios;
    }

    public void setPrivilegios(List<String> privilegios) {
        this.privilegios = privilegios;
    }

    public List<String> getProyectos() {
        return proyectos;
    }

    public void setProyectos(List<String> proyectos) {
        this.proyectos = proyectos;
    }

    public List<String> getTareasAsignadas() {
        return tareasAsignadas;
    }

    public void setTareasAsignadas(List<String> tareasAsignadas) {
        this.tareasAsignadas = tareasAsignadas;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public LocalDateTime getUltimaCambioContrasena() {
        return ultimaCambioContrasena;
    }

    public void setUltimaCambioContrasena(LocalDateTime ultimaCambioContrasena) {
        this.ultimaCambioContrasena = ultimaCambioContrasena;
    }

    // Métodos de utilidad
    public void addProyecto(String idProyecto) {
        if (!this.proyectos.contains(idProyecto)) {
            this.proyectos.add(idProyecto);
        }
    }

    public void addTareaAsignada(String idTarea) {
        if (!this.tareasAsignadas.contains(idTarea)) {
            this.tareasAsignadas.add(idTarea);
        }
    }
}
