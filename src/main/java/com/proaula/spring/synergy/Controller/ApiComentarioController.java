package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ComentarioService;
import com.proaula.spring.synergy.Service.ProyectoService;

@RestController
@RequestMapping("/api/comentarios")
@PreAuthorize("isAuthenticated()")
public class ApiComentarioController {

    private final ComentarioService comentarioService;
    private final ProyectoService proyectoService;

    public ApiComentarioController(ComentarioService comentarioService, ProyectoService proyectoService) {
        this.comentarioService = comentarioService;
        this.proyectoService = proyectoService;
    }

    @GetMapping("/proyecto/{proyectoId}")
    public List<Comentario> listarPorProyecto(@PathVariable String proyectoId) {
        return comentarioService.listarPorProyecto(proyectoId);
    }

    @PostMapping("/proyecto/{proyectoId}")
    public ResponseEntity<Comentario> crear(@PathVariable String proyectoId, @RequestBody Comentario comentario) {
        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        if (proyecto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        if (comentario.getContenido() == null || comentario.getContenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario no puede estar vacío");
        }
        comentario.setId(null);
        comentario.setIdProyecto(proyectoId);
        comentario.setIdUsuario(usuarioActual().getId());
        Comentario creado = comentarioService.guardarComentario(comentario);
        proyecto.addComentario(creado.getId());
        proyectoService.guardar(proyecto, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        Comentario comentario = comentarioService.obtenerPorId(id);
        if (comentario == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comentario no encontrado");
        Usuarios usuario = usuarioActual();
        if (!usuario.getId().equals(comentario.getIdUsuario()) && !"ADMINISTRADOR".equals(usuario.getRol())) {
            throw new AccessDeniedException("Solo el autor puede eliminar este comentario");
        }
        comentarioService.eliminarComentario(id);
        Proyecto proyecto = proyectoService.buscarPorId(comentario.getIdProyecto());
        if (proyecto != null) {
            proyecto.getComentarios().remove(id);
            proyectoService.guardar(proyecto, null);
        }
        return ResponseEntity.noContent().build();
    }

    private Usuarios usuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuarios usuario) return usuario;
        throw new AccessDeniedException("No hay un usuario autenticado");
    }
}
