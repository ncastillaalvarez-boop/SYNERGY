package com.proaula.spring.synergy.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Repository.ComentarioRepository;
import com.proaula.spring.synergy.Repository.ProyectoRepository;

@Service
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    public List<Comentario> listarPorProyecto(String proyectoId) {
        return comentarioRepository.findByIdProyecto(proyectoId);
    }

    public void guardar(String proyectoId, String texto, String idUsuario) {
        // Validar que proyecto exista
        if (!proyectoRepository.existsById(proyectoId)) {
            throw new RuntimeException("Proyecto no encontrado");
        }

        Comentario c = new Comentario();
        c.setContenido(texto);
        c.setFecha(LocalDateTime.now());
        c.setIdProyecto(proyectoId);
        c.setIdUsuario(idUsuario);

        comentarioRepository.save(c);
    }

    public Comentario guardarComentario(Comentario comentario) {
        return comentarioRepository.save(comentario);
    }

    public List<Comentario> listarComentarios() {
        return comentarioRepository.findAll();
    }

    public Comentario obtenerPorId(String id) {
        return comentarioRepository.findById(id).orElse(null);
    }

    public void eliminarComentario(String id) {
        comentarioRepository.deleteById(id);
    }
}

