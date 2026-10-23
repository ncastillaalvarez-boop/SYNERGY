package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private RolPrivilegioService rolPrivilegioService;

    public List<Usuarios> obtenerTodosUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuarios obtenerUsuarioPorId(String id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public Usuarios obtenerUsuarioPorCorreo(String correo) {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }
        return usuario;
    }

    public Usuarios crearUsuario(Usuarios usuario) {
        String correo = usuario.getCorreo() == null ? "" : usuario.getCorreo().trim().toLowerCase();
        if (correo.isEmpty() || !correo.contains("@")) {
            throw new IllegalArgumentException("Escribe un correo válido");
        }
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new IllegalArgumentException("Escribe el nombre del usuario");
        }
        String problema = com.proaula.spring.synergy.Security.PoliticaContrasena.validar(usuario.getContrasena());
        if (problema != null) {
            throw new IllegalArgumentException(problema);
        }
        if (usuarioRepository.findByCorreo(correo) != null) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }
        usuario.setId(null);
        usuario.setCorreo(correo);
        usuario.setNombre(usuario.getNombre().trim());
        String rol = usuario.getRol() == null ? "" : usuario.getRol().trim().toUpperCase();
        usuario.setRol(java.util.Set.of("ADMINISTRADOR", "LIDER", "PARTICIPANTE").contains(rol) ? rol : "PARTICIPANTE");

        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuario.setActivo(true);
        usuario.setEstado("ACTIVO");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setUltimaCambioContrasena(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    public Usuarios actualizarUsuario(String id, Usuarios usuarioActualizado) {
        Usuarios usuario = obtenerUsuarioPorId(id);
        usuario.setNombre(usuarioActualizado.getNombre());
        if (usuarioActualizado.getCorreo() != null && !usuario.getCorreo().equalsIgnoreCase(usuarioActualizado.getCorreo().trim())) {
            String nuevo = usuarioActualizado.getCorreo().trim().toLowerCase();
            if (usuarioRepository.findByCorreo(nuevo) != null) {
                throw new IllegalArgumentException("El correo ya está registrado");
            }
            usuario.setCorreo(nuevo);
        }
        if (usuarioActualizado.getEstado() != null) {
            usuario.setEstado(usuarioActualizado.getEstado());
        }
        if (usuarioActualizado.getActivo() != null) {
            usuario.setActivo(usuarioActualizado.getActivo());
        }
        return usuarioRepository.save(usuario);
    }

    public void cambiarRolUsuario(String usuarioId, String nuevoRol) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        
        // Validar que el rol exista
        rolPrivilegioService.obtenerRolPorNombre(nuevoRol);
        
        usuario.setRol(nuevoRol);
        usuarioRepository.save(usuario);
    }

    public void desactivarUsuario(String usuarioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.setActivo(false);
        usuario.setEstado("INACTIVO");
        usuarioRepository.save(usuario);
    }

    public void activarUsuario(String usuarioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.setActivo(true);
        usuario.setEstado("ACTIVO");
        usuarioRepository.save(usuario);
    }

    /**
     * Elimina la cuenta y la quita de proyectos, participaciones y tareas.
     * Si lidera proyectos NO se elimina (quedarían proyectos sin líder): primero
     * hay que eliminar esos proyectos o cambiarle el rol/desactivarlo.
     */
    public void eliminarUsuario(String usuarioId) {
        long liderados = mongoTemplate.count(Query.query(Criteria.where("idLider").is(usuarioId)), Proyecto.class);
        if (liderados > 0) {
            throw new IllegalStateException("Este usuario lidera " + liderados
                    + " proyecto(s). Elimínalos o desactiva la cuenta en lugar de borrarla.");
        }
        mongoTemplate.remove(Query.query(Criteria.where("idUsuario").is(usuarioId)), Participacion.class);
        mongoTemplate.updateMulti(Query.query(Criteria.where("participantes").is(usuarioId)),
                new Update().pull("participantes", usuarioId), Proyecto.class);
        mongoTemplate.updateMulti(Query.query(Criteria.where("usuariosAsignados").is(usuarioId)),
                new Update().pull("usuariosAsignados", usuarioId), Tarea.class);
        usuarioRepository.deleteById(usuarioId);
    }

    public List<Usuarios> obtenerUsuariosPorRol(String rol) {
        return usuarioRepository.findByRol(rol);
    }

    public List<Usuarios> obtenerUsuariosActivos() {
        return usuarioRepository.findByActivo(true);
    }

    public List<Usuarios> obtenerUsuariosPorEstado(String estado) {
        return usuarioRepository.findByEstado(estado);
    }

    public void agregarPrivilegioAlUsuario(String usuarioId, String privilegioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        if (!usuario.getPrivilegios().contains(privilegioId)) {
            usuario.getPrivilegios().add(privilegioId);
            usuarioRepository.save(usuario);
        }
    }

    public void removerPrivilegioDelUsuario(String usuarioId, String privilegioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.getPrivilegios().remove(privilegioId);
        usuarioRepository.save(usuario);
    }

    public boolean usuarioTienePrivilegio(String usuarioId, String nombrePrivilegio) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        return rolPrivilegioService.usuarioTienePrivilegio(usuario.getRol(), nombrePrivilegio);
    }

    public void actualizarUltimoAcceso(String usuarioId) {
        Usuarios usuario = obtenerUsuarioPorId(usuarioId);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    public Usuarios buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public Usuarios guardarUsuario(Usuarios usuario) {
        return usuarioRepository.save(usuario);
    }
}
