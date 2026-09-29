package com.proaula.spring.synergy.Controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.EntregaTareaService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;

@RestController
@RequestMapping("/api/entregas")
@PreAuthorize("isAuthenticated()")
public class ApiEntregaTareaController {

    private final EntregaTareaService entregaService;
    private final TareaService tareaService;
    private final ProyectoService proyectoService;

    public ApiEntregaTareaController(EntregaTareaService entregaService, TareaService tareaService,
            ProyectoService proyectoService) {
        this.entregaService = entregaService;
        this.tareaService = tareaService;
        this.proyectoService = proyectoService;
    }

    @GetMapping("/tarea/{tareaId}")
    public List<EntregaTarea> listar(@PathVariable String tareaId) {
        validarAccesoTarea(buscarTarea(tareaId), false);
        return entregaService.obtenerEntregasPorTarea(tareaId);
    }

    @PostMapping("/tarea/{tareaId}")
    public ResponseEntity<Void> entregar(@PathVariable String tareaId, @RequestParam("archivo") MultipartFile archivo)
            throws IOException {
        Tarea tarea = buscarTarea(tareaId);
        validarAccesoTarea(tarea, true);
        if (archivo.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes seleccionar un archivo");
        entregaService.guardarEntrega(tareaId, usuarioActual().getId(), archivo);
        tareaService.cambiarEstado(tareaId, "COMPLETADA");
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private Tarea buscarTarea(String id) {
        Tarea tarea = tareaService.obtenerTareaPorId(id);
        if (tarea == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada");
        return tarea;
    }

    private void validarAccesoTarea(Tarea tarea, boolean esEntrega) {
        Usuarios usuario = usuarioActual();
        Proyecto proyecto = proyectoService.buscarPorId(tarea.getIdProyecto());
        boolean esAdministrador = "ADMINISTRADOR".equals(usuario.getRol());
        boolean esLider = proyecto != null && usuario.getId().equals(proyecto.getIdLider());
        boolean estaAsignado = tarea.getUsuariosAsignados().contains(usuario.getId());
        if ((esEntrega && !estaAsignado && !esAdministrador) || (!esEntrega && !estaAsignado && !esLider && !esAdministrador)) {
            throw new AccessDeniedException("No tienes acceso a las entregas de esta tarea");
        }
    }

    private Usuarios usuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuarios usuario) return usuario;
        throw new AccessDeniedException("No hay un usuario autenticado");
    }
}
