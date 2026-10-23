package com.proaula.spring.synergy.Service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ParticipacionRepository;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Service.ParticipacionService;

@Service
public class ParticipacionServiceImpl implements ParticipacionService {

    @Autowired
    private ParticipacionRepository participacionRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private com.proaula.spring.synergy.Service.NotificacionService notificaciones;

    public boolean inscribirUsuario(String proyectoId, String usuarioId) {
        
        // Validar que usuario y proyecto existan
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RuntimeException("Usuario no existe");
        }

        Proyecto destino = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no existe"));
        // Solo se puede entrar a proyectos publicados y en curso
        if (!"ACTIVO".equalsIgnoreCase(destino.getEstado())) {
            throw new RuntimeException("Este proyecto no está recibiendo voluntarios en este momento");
        }
        if (usuarioId.equals(destino.getIdLider())) {
            throw new RuntimeException("Ya eres el líder de este proyecto");
        }

        // Evitar duplicados
        Participacion participacionExistente = participacionRepository
                .findByIdUsuarioAndIdProyecto(usuarioId, proyectoId);

        if (participacionExistente != null) {
            return false; // YA ESTABA INSCRITO
        }

        // Crear participación
        Participacion participacion = new Participacion();
        participacion.setIdUsuario(usuarioId);
        participacion.setIdProyecto(proyectoId);
        Proyecto proyecto = proyectoRepository.findById(proyectoId).orElseThrow();
        participacion.setRol(usuarioId.equals(proyecto.getIdLider()) ? "LIDER" : "PARTICIPANTE");
        participacionRepository.save(participacion);

        // Mantener sincronizadas las referencias denormalizadas de MongoDB.
        proyecto.addParticipante(usuarioId);
        proyectoRepository.save(proyecto);
        Usuarios usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.addProyecto(proyectoId);
        usuarioRepository.save(usuario);

        if (notificaciones != null) {
            notificaciones.notificar(proyecto.getIdLider(), "VOLUNTARIO", "Nuevo voluntario en " + proyecto.getNombre(),
                    usuario.getNombre() + " se inscribió en tu proyecto.", "/lider/usuarios?proyecto=" + proyecto.getId());
        }
        return true; // INSCRIPCIÓN EXITOSA
    }

    public List<Participacion> listarParticipantes(String proyectoId) {
        return participacionRepository.findByIdProyecto(proyectoId);
    }

    public boolean yaInscrito(String proyectoId, String usuarioId) {
        return participacionRepository.findByIdUsuarioAndIdProyecto(usuarioId, proyectoId) != null;
    }

    public void eliminarParticipacion(String proyectoId, String usuarioId) {
        Participacion participacion = participacionRepository
                .findByIdUsuarioAndIdProyecto(usuarioId, proyectoId);
        if (participacion != null) {
            participacionRepository.delete(participacion);
            Proyecto proyecto = proyectoRepository.findById(proyectoId).orElse(null);
            if (proyecto != null) {
                proyecto.removeParticipante(usuarioId);
                proyectoRepository.save(proyecto);
            }
            Usuarios usuario = usuarioRepository.findById(usuarioId).orElse(null);
            if (usuario != null) {
                usuario.getProyectos().remove(proyectoId);
                usuarioRepository.save(usuario);
            }
        }
    }

    public List<Proyecto> listarProyectosPorUsuario(String usuarioId) {
        List<Participacion> participaciones = participacionRepository.findByIdUsuario(usuarioId);
        // Una sola consulta en vez de una por proyecto
        List<String> ids = participaciones.stream().map(Participacion::getIdProyecto).distinct().collect(Collectors.toList());
        return proyectoRepository.findAllById(ids);
    }

    public List<Usuarios> listarUsuariosPorProyecto(String proyectoId) {
        List<Participacion> participaciones = participacionRepository.findByIdProyecto(proyectoId);
        List<String> ids = participaciones.stream().map(Participacion::getIdUsuario).distinct().collect(Collectors.toList());
        return usuarioRepository.findAllById(ids);
    }

    public List<Participacion> listar() {
        return participacionRepository.findAll();
    }

    public Participacion guardar(Participacion participacion) {
        return participacionRepository.save(participacion);
    }

    public void eliminar(String id) {
        participacionRepository.deleteById(id);
    }
}
