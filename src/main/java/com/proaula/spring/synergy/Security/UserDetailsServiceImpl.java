package com.proaula.spring.synergy.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.RolRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuarios usuario = usuarioRepository.findByCorreo(correo);

        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado con email: " + correo);
        }

        cargarNombreRol(usuario);
        return usuario;
    }

    public UserDetails loadUserById(String id) throws UsernameNotFoundException {
        Usuarios usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        cargarNombreRol(usuario);
        return usuario;
    }

    private void cargarNombreRol(Usuarios usuario) {
        if (usuario.getIdRol() != null) {
            rolRepository.findById(usuario.getIdRol())
                    .ifPresent(rol -> usuario.setRol(rol.getNombre()));
        }
    }
}
