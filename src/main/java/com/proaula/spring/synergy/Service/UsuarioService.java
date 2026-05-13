package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Rol;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.RolRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RolPrivilegioService rolPrivilegioService;

    @Autowired
    private RolRepository rolRepository;

    // Popula el nombre del rol (transient) leyendo la colección roles
    private void cargarRolNombre(Usuarios usuario) {
        if (usuario != null && usuario.getIdRol() != null) {
            rolRepository.findById(usuario.getIdRol())
                    .ifPresent(rol -> usuario.setRol(rol.getNombre()));
        }
    }

    public List<Usuarios> obtenerTodosUsuarios() {
        List<Usuarios> usuarios = usuarioRepository.findAll();
        usuarios.forEach(this::cargarRolNombre);
        return usuarios;
    }

    public Usuarios obtenerUsuarioPorId(String id) {
        Usuarios usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        cargarRolNombre(usuario);
        return usuario;
    }

    public Usuarios obtenerUsuarioPorCorreo(String correo) {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }
        cargarRolNombre(usuario);
        return usuario;
    }

    public Usuarios crearUsuario(Usuarios usuario) {
        if (usuarioRepository.findByCorreo(usuario.getCorreo()) != null) {
            throw new RuntimeException("El correo ya está registrado");
        }

        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuario.setEstado("ACTIVO");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setUltimoCambioContrasena(LocalDateTime.now());

        Usuarios guardado = usuarioRepository.save(usuario);
        cargarRolNombre(guardado);
        return guardado;
    }

    public Usuarios actualizarUsuario(String id, Usuarios usuarioActualizado) {
        Usuarios usuario = obtenerUsuarioPorId(id);
        usuario.setNombre(usuarioActualizado.getNombre());
        if (!usuario.getCorreo().equals(usuarioActualizado.getCorreo())) {
            if (usuarioRepository.findByCorreo(usuarioActualizado.getCorreo()) != null) {
                throw new RuntimeException("El correo ya está registrado");
            }
            usuario.setCorreo(usuarioActualizado.getCorreo());
        }
        usuario.setEstado(usuarioActualizado.getEstado());
        Usuarios guardado = usuarioRepository.save(usuario);
        cargarRolNombre(guardado);
        return guardado;
    }

    public void cambiarRolUsuario(String usuarioId, String nombreRol) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        Rol rol = rolPrivilegioService.obtenerRolPorNombre(nombreRol);
        usuario.setIdRol(rol.getId());
        usuarioRepository.save(usuario);
    }

    public void desactivarUsuario(String usuarioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.setEstado("INACTIVO");
        usuarioRepository.save(usuario);
    }

    public void activarUsuario(String usuarioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.setEstado("ACTIVO");
        usuarioRepository.save(usuario);
    }

    public void eliminarUsuario(String usuarioId) {
        usuarioRepository.deleteById(usuarioId);
    }

    public List<Usuarios> obtenerUsuariosPorRol(String nombreRol) {
        try {
            Rol rol = rolPrivilegioService.obtenerRolPorNombre(nombreRol);
            List<Usuarios> usuarios = usuarioRepository.findByIdRol(rol.getId());
            usuarios.forEach(this::cargarRolNombre);
            return usuarios;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Usuarios> obtenerUsuariosActivos() {
        List<Usuarios> usuarios = usuarioRepository.findByEstado("ACTIVO");
        usuarios.forEach(this::cargarRolNombre);
        return usuarios;
    }

    public List<Usuarios> obtenerUsuariosPorEstado(String estado) {
        List<Usuarios> usuarios = usuarioRepository.findByEstado(estado);
        usuarios.forEach(this::cargarRolNombre);
        return usuarios;
    }

    // Los privilegios ahora viven en el Rol, no en el usuario
    public void agregarPrivilegioAlUsuario(String usuarioId, String privilegioId) {
        // privilegios de usuario eliminados del modelo; gestionar desde el Rol
    }

    public void removerPrivilegioDelUsuario(String usuarioId, String privilegioId) {
        // privilegios de usuario eliminados del modelo; gestionar desde el Rol
    }

    public boolean usuarioTienePrivilegio(String usuarioId, String nombrePrivilegio) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        return rolPrivilegioService.usuarioTienePrivilegio(usuario.getIdRol(), nombrePrivilegio);
    }

    public void actualizarUltimoAcceso(String usuarioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    public Usuarios buscarPorCorreo(String correo) {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);
        cargarRolNombre(usuario);
        return usuario;
    }

    public Usuarios guardarUsuario(Usuarios usuario) {
        Usuarios guardado = usuarioRepository.save(usuario);
        cargarRolNombre(guardado);
        return guardado;
    }

    public String obtenerIdRolPorNombre(String nombreRol) {
        return rolPrivilegioService.obtenerRolPorNombre(nombreRol).getId();
    }
}
