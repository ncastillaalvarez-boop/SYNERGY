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

    public boolean inscribirUsuario(String proyectoId, String usuarioId) {
        
        // Validar que usuario y proyecto existan
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RuntimeException("Usuario no existe");
        }

        if (!proyectoRepository.existsById(proyectoId)) {
            throw new RuntimeException("Proyecto no existe");
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
        participacionRepository.save(participacion);

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
        }
    }

    public List<Proyecto> listarProyectosPorUsuario(String usuarioId) {
        List<Participacion> participaciones = participacionRepository.findByIdUsuario(usuarioId);
        return participaciones.stream()
                .map(p -> proyectoRepository.findById(p.getIdProyecto()).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toList());
    }

    public List<Usuarios> listarUsuariosPorProyecto(String proyectoId) {
        List<Participacion> participaciones = participacionRepository.findByIdProyecto(proyectoId);
        return participaciones.stream()
                .map(p -> usuarioRepository.findById(p.getIdUsuario()).orElse(null))
                .filter(u -> u != null)
                .collect(Collectors.toList());
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
