package com.proaula.spring.synergy.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;

@RestController
@RequestMapping("/api/participaciones")
@PreAuthorize("isAuthenticated()")
public class ApiParticipacionController {

    private final ParticipacionService participacionService;
    private final ProyectoService proyectoService;

    public ApiParticipacionController(ParticipacionService participacionService, ProyectoService proyectoService) {
        this.participacionService = participacionService;
        this.proyectoService = proyectoService;
    }

    @GetMapping("/mias")
    public List<Proyecto> misProyectos() {
        return participacionService.listarProyectosPorUsuario(usuarioActual().getId());
    }

    /** Participantes de un proyecto: solo sus integrantes (antes cualquier usuario). */
    @GetMapping("/proyecto/{proyectoId}")
    public List<Participacion> participantes(@PathVariable String proyectoId) {
        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        if (proyecto == null) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        com.proaula.spring.synergy.Security.ApiAcceso.exigirIntegrante(proyecto, "Solo los integrantes ven la lista de participantes");
        return participacionService.listarParticipantes(proyectoId);
    }

    @GetMapping("/proyecto/{proyectoId}/inscrito")
    public Map<String, Boolean> estaInscrito(@PathVariable String proyectoId) {
        return Map.of("inscrito", participacionService.yaInscrito(proyectoId, usuarioActual().getId()));
    }

    @PostMapping("/proyecto/{proyectoId}")
    public ResponseEntity<Map<String, Object>> unirse(@PathVariable String proyectoId) {
        if (proyectoService.buscarPorId(proyectoId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "Proyecto no encontrado"));
        }
        boolean creado;
        try {
            creado = participacionService.inscribirUsuario(proyectoId, usuarioActual().getId());
        } catch (RuntimeException e) {
            // p. ej. "Este proyecto no está recibiendo voluntarios en este momento"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage(), "mensaje", e.getMessage()));
        }
        return ResponseEntity.status(creado ? HttpStatus.CREATED : HttpStatus.OK)
                .body(Map.of("inscrito", creado, "mensaje", creado ? "Participación creada" : "Ya estabas inscrito"));
    }

    @DeleteMapping("/proyecto/{proyectoId}")
    public ResponseEntity<Void> abandonar(@PathVariable String proyectoId) {
        Usuarios usuario = usuarioActual();
        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        if (proyecto != null && usuario.getId().equals(proyecto.getIdLider())) {
            throw new AccessDeniedException("El líder no puede abandonar su propio proyecto");
        }
        participacionService.eliminarParticipacion(proyectoId, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    private Usuarios usuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuarios usuario) return usuario;
        throw new AccessDeniedException("No hay un usuario autenticado");
    }
}
