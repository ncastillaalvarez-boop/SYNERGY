package com.proaula.spring.synergy.Service.Impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.UsuarioRepository;

@Service
public class UsuarioServiceImpl {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuarios> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuarios guardarUsuario(Usuarios usuario) {
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuarios> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuarios> buscarPorCorreo(String correo) {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);
        return Optional.ofNullable(usuario);
    }

    public void eliminarUsuario(String id) {
        usuarioRepository.deleteById(id);
    }

    public List<Usuarios> listarLideres() {
        return usuarioRepository.findByRol("LIDER");
    }

    public List<Usuarios> listarActivos() {
        return usuarioRepository.findByActivo(true);
    }

    public Usuarios crearUsuario(Usuarios usuario) {
        if (usuarioRepository.findByCorreo(usuario.getCorreo()) != null) {
            throw new RuntimeException("El correo ya está registrado");
        }
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        usuario.setActivo(true);
        usuario.setEstado("ACTIVO");
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setUltimaCambioContrasena(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }
}
