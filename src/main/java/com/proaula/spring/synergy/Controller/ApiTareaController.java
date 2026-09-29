package com.proaula.spring.synergy.Controller;

import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;
import com.proaula.spring.synergy.Service.UsuarioService;

@RestController
@RequestMapping("/api/tareas")
@PreAuthorize("isAuthenticated()")
public class ApiTareaController {

    private final TareaService tareaService;
    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;

    public ApiTareaController(TareaService tareaService, ProyectoService proyectoService, UsuarioService usuarioService) {
        this.tareaService = tareaService;
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/mias")
    public List<Tarea> misTareas() {
        return tareaService.obtenerTareasPorUsuario(usuarioActual().getId());
    }

    @GetMapping("/gestion")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public List<Tarea> tareasGestionadas() {
        List<Tarea> resultado = new ArrayList<>();
        for (Proyecto proyecto : proyectoService.obtenerProyectosDeLider(usuarioActual().getId())) {
            resultado.addAll(tareaService.obtenerTareasPorProyecto(proyecto.getId()));
        }
        return resultado;
    }

    @GetMapping("/proyecto/{proyectoId}")
    public List<Tarea> porProyecto(@PathVariable String proyectoId) {
        return tareaService.obtenerTareasPorProyecto(proyectoId);
    }

    @GetMapping("/{id}")
    public Tarea obtener(@PathVariable String id) {
        return buscar(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public ResponseEntity<Tarea> crear(@RequestBody Tarea tarea) {
        Proyecto proyecto = buscarProyecto(tarea.getIdProyecto());
        exigirLiderDelProyecto(proyecto);
        tarea.setId(null);
        Tarea creada = tareaService.guardarTarea(tarea, tarea.getUsuariosAsignados());
        proyecto.addTarea(creada.getId());
        proyectoService.guardar(proyecto, null);
        sincronizarAsignaciones(creada);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public Tarea actualizar(@PathVariable String id, @RequestBody Tarea cambios) {
        Tarea existente = buscar(id);
        Proyecto proyecto = buscarProyecto(existente.getIdProyecto());
        exigirLiderDelProyecto(proyecto);
        cambios.setId(id);
        cambios.setIdProyecto(existente.getIdProyecto());
        cambios.setFechaCreacion(existente.getFechaCreacion());
        Tarea actualizada = tareaService.guardarTarea(cambios, cambios.getUsuariosAsignados());
        sincronizarAsignaciones(actualizada);
        return actualizada;
    }

    @PutMapping("/{id}/estado")
    public Tarea actualizarEstado(@PathVariable String id, @RequestBody Map<String, String> body) {
        Tarea tarea = buscar(id);
        Usuarios usuario = usuarioActual();
        Proyecto proyecto = buscarProyecto(tarea.getIdProyecto());
        if (!"ADMINISTRADOR".equals(usuario.getRol())
                && !usuario.getId().equals(proyecto.getIdLider())
                && !tarea.getUsuariosAsignados().contains(usuario.getId())) {
            throw new AccessDeniedException("No puedes actualizar esta tarea");
        }
        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        tareaService.cambiarEstado(id, estado.toUpperCase());
        return buscar(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        Tarea tarea = buscar(id);
        exigirLiderDelProyecto(buscarProyecto(tarea.getIdProyecto()));
        tareaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private void sincronizarAsignaciones(Tarea tarea) {
        for (String usuarioId : tarea.getUsuariosAsignados()) {
            try {
                Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId);
                usuario.addTareaAsignada(tarea.getId());
                usuarioService.guardarUsuario(usuario);
            } catch (RuntimeException ignored) {
                // Un identificador inválido no impide que el líder recupere la tarea.
            }
        }
    }

    private Tarea buscar(String id) {
        Tarea tarea = tareaService.obtenerTareaPorId(id);
        if (tarea == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        return tarea;
    }

    private Proyecto buscarProyecto(String id) {
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        return proyecto;
    }

    private Usuarios usuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuarios usuario) return usuario;
        throw new AccessDeniedException("No hay un usuario autenticado");
    }

    private void exigirLiderDelProyecto(Proyecto proyecto) {
        Usuarios usuario = usuarioActual();
        if (!"ADMINISTRADOR".equals(usuario.getRol()) && !usuario.getId().equals(proyecto.getIdLider())) {
            throw new AccessDeniedException("Solo el líder del proyecto puede gestionar sus tareas");
        }
    }
}
