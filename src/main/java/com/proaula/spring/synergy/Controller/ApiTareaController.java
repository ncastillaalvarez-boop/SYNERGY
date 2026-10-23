package com.proaula.spring.synergy.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final com.proaula.spring.synergy.Service.NotificacionService notificaciones;

    public ApiTareaController(TareaService tareaService, ProyectoService proyectoService, UsuarioService usuarioService,
                              com.proaula.spring.synergy.Service.NotificacionService notificaciones) {
        this.tareaService = tareaService;
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
        this.notificaciones = notificaciones;
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

    /** Tareas de un proyecto: solo sus integrantes (antes cualquier usuario las veía). */
    @GetMapping("/proyecto/{proyectoId}")
    public List<Tarea> porProyecto(@PathVariable String proyectoId) {
        Proyecto proyecto = buscarProyecto(proyectoId);
        com.proaula.spring.synergy.Security.ApiAcceso.exigirIntegrante(proyecto, "Solo los integrantes del proyecto ven sus tareas");
        return tareaService.obtenerTareasPorProyecto(proyectoId);
    }

    @GetMapping("/{id}")
    public Tarea obtener(@PathVariable String id) {
        Tarea tarea = buscar(id);
        Proyecto proyecto = proyectoService.buscarPorId(tarea.getIdProyecto());
        if (!com.proaula.spring.synergy.Security.ApiAcceso.puedeVerTarea(tarea, proyecto, usuarioActual())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        }
        return tarea;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public ResponseEntity<Tarea> crear(@RequestBody Tarea tarea) {
        Proyecto proyecto = buscarProyecto(tarea.getIdProyecto());
        exigirLiderDelProyecto(proyecto);
        String error = com.proaula.spring.synergy.Service.ValidacionService.validarTarea(tarea.getTitulo(), tarea.getFechaEntrega(), proyecto, true);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        tarea.setId(null);
        tarea.setTitulo(tarea.getTitulo().trim());
        tarea.setEstado("PENDIENTE");
        tarea.setUsuariosAsignados(soloIntegrantes(tarea.getUsuariosAsignados(), proyecto));
        Tarea creada = tareaService.guardarTarea(tarea, tarea.getUsuariosAsignados());
        proyecto.addTarea(creada.getId());
        proyectoService.guardar(proyecto, null);
        sincronizarAsignaciones(creada);
        notificaciones.notificarVarios(creada.getUsuariosAsignados(), "TAREA", "Nueva tarea: " + creada.getTitulo(),
                proyecto.getNombre() + " · vence el " + creada.getFechaEntrega(), "/tareas/buzon");
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public Tarea actualizar(@PathVariable String id, @RequestBody Tarea cambios) {
        Tarea existente = buscar(id);
        Proyecto proyecto = buscarProyecto(existente.getIdProyecto());
        exigirLiderDelProyecto(proyecto);
        // Solo se copian los campos enviados (antes un campo ausente borraba el valor guardado)
        if (cambios.getTitulo() != null) existente.setTitulo(cambios.getTitulo().trim());
        if (cambios.getDescripcion() != null) existente.setDescripcion(cambios.getDescripcion());
        if (cambios.getFechaEntrega() != null) existente.setFechaEntrega(cambios.getFechaEntrega());
        if (cambios.getEstado() != null && ESTADOS.contains(cambios.getEstado().toUpperCase())) {
            existente.setEstado(cambios.getEstado().toUpperCase());
        }
        String error = com.proaula.spring.synergy.Service.ValidacionService.validarTarea(existente.getTitulo(), existente.getFechaEntrega(), proyecto, false);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        List<String> asignados = cambios.getUsuariosAsignados() != null
                ? soloIntegrantes(cambios.getUsuariosAsignados(), proyecto) : existente.getUsuariosAsignados();
        Tarea actualizada = tareaService.guardarTarea(existente, asignados);
        sincronizarAsignaciones(actualizada);
        return actualizada;
    }

    @PutMapping("/{id}/estado")
    public Tarea actualizarEstado(@PathVariable String id, @RequestBody Map<String, String> body) {
        Tarea tarea = buscar(id);
        Usuarios usuario = usuarioActual();
        Proyecto proyecto = buscarProyecto(tarea.getIdProyecto());
        if (!com.proaula.spring.synergy.Security.ApiAcceso.gestiona(proyecto, usuario)
                && !tarea.getUsuariosAsignados().contains(usuario.getId())) {
            throw new AccessDeniedException("No puedes actualizar esta tarea");
        }
        String estado = body.get("estado");
        if (estado == null || !ESTADOS.contains(estado.trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido. Usa PENDIENTE, EN_PROGRESO, COMPLETADA o CANCELADA.");
        }
        tareaService.cambiarEstado(id, estado.trim().toUpperCase());
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

    private static final java.util.Set<String> ESTADOS = java.util.Set.of("PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA");

    /** Solo se pueden asignar voluntarios inscritos en el proyecto. */
    private static List<String> soloIntegrantes(List<String> ids, Proyecto proyecto) {
        List<String> resultado = new ArrayList<>();
        if (ids != null) {
            for (String uid : ids) {
                if (uid != null && proyecto.getParticipantes().contains(uid) && !resultado.contains(uid)) {
                    resultado.add(uid);
                }
            }
        }
        return resultado;
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
        return com.proaula.spring.synergy.Security.ApiAcceso.usuarioActual();
    }

    private void exigirLiderDelProyecto(Proyecto proyecto) {
        com.proaula.spring.synergy.Security.ApiAcceso.exigirGestion(proyecto, "Solo el líder del proyecto puede gestionar sus tareas");
    }
}
