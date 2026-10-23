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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;

/** API JSON para el cliente móvil. Las rutas Thymeleaf existentes se mantienen intactas. */
@RestController
@RequestMapping("/api/proyectos")
@PreAuthorize("isAuthenticated()")
public class ApiProyectoController {

    private final ProyectoService proyectoService;
    private final ParticipacionService participacionService;
    private final com.proaula.spring.synergy.Service.RiesgoProyectoService riesgoService;

    public ApiProyectoController(ProyectoService proyectoService, ParticipacionService participacionService,
                                 com.proaula.spring.synergy.Service.RiesgoProyectoService riesgoService) {
        this.proyectoService = proyectoService;
        this.participacionService = participacionService;
        this.riesgoService = riesgoService;
    }

    @GetMapping
    public List<Proyecto> listar() {
        return proyectoService.listar();
    }

    @GetMapping("/{id}")
    public Proyecto obtener(@PathVariable String id) {
        return buscar(id);
    }

    /** Riesgo del proyecto según el modelo predictivo (para la app Flutter). */
    @GetMapping("/{id}/riesgo")
    public java.util.Map<String, Object> riesgo(@PathVariable String id) {
        Proyecto p = buscar(id);
        Usuarios usuario = usuarioActual();
        if (!"ADMINISTRADOR".equals(usuario.getRol()) && !usuario.getId().equals(p.getIdLider())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Solo el líder del proyecto o el administrador pueden ver el riesgo");
        }
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("idProyecto", p.getId());
        r.put("estado", p.getEstado());
        if (!"ACTIVO".equals(p.getEstado())) {
            r.put("nivel", null);
            r.put("motivos", List.of("El riesgo solo se calcula para proyectos activos"));
            return r;
        }
        var e = riesgoService.evaluar(p);
        r.put("nivel", e.nivel());
        r.put("probabilidad", Math.round(e.probabilidad() * 1000) / 1000.0);
        r.put("probabilidades", java.util.Map.of("Bajo", e.probabilidades()[0], "Medio", e.probabilidades()[1], "Alto", e.probabilidades()[2]));
        r.put("motivos", e.motivos());
        r.put("sugerencia", e.sugerencia());
        return r;
    }

    @GetMapping("/mis-proyectos")
    public List<Proyecto> misProyectos() {
        Usuarios usuario = usuarioActual();
        return participacionService.listarProyectosPorUsuario(usuario.getId());
    }

    @GetMapping("/lider/mios")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public List<Proyecto> proyectosDelLider() {
        return proyectoService.obtenerProyectosDeLider(usuarioActual().getId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public ResponseEntity<Proyecto> crear(@RequestBody Proyecto proyecto) {
        Usuarios usuario = usuarioActual();
        proyecto.setId(null);
        proyecto.setCodigo(null);
        proyecto.setIdLider(usuario.getId());
        proyecto.setEstado("ACTIVO");
        // guardar() ya agrega al líder como integrante y asigna el código SYN-AAAA-NNNNN
        Proyecto creado = proyectoService.guardar(proyecto, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public Proyecto actualizar(@PathVariable String id, @RequestBody Proyecto cambios) {
        Proyecto existente = buscar(id);
        exigirPropietarioOAdministrador(existente);
        // Se copian solo los campos editables: antes se reemplazaba el documento
        // completo y se perdían participantes, tareas, comentarios y código.
        if (cambios.getNombre() != null && !cambios.getNombre().isBlank()) existente.setNombre(cambios.getNombre());
        if (cambios.getDescripcion() != null && !cambios.getDescripcion().isBlank()) existente.setDescripcion(cambios.getDescripcion());
        if (cambios.getFechaInicio() != null) existente.setFechaInicio(cambios.getFechaInicio());
        if (cambios.getFechaFin() != null) existente.setFechaFin(cambios.getFechaFin());
        if (cambios.getCategoria() != null) existente.setCategoria(cambios.getCategoria());
        if (cambios.getTipoOrganizacion() != null) existente.setTipoOrganizacion(cambios.getTipoOrganizacion());
        if (cambios.getUbicacion() != null) existente.setUbicacion(cambios.getUbicacion());
        if (cambios.getAudiencia() != null) existente.setAudiencia(cambios.getAudiencia());
        return proyectoService.guardar(existente, null);
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public Proyecto actualizarEstado(@PathVariable String id, @RequestBody Map<String, String> body) {
        Proyecto proyecto = buscar(id);
        exigirPropietarioOAdministrador(proyecto);
        String estado = body.get("estado");
        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }
        String nuevo = estado.trim().toUpperCase();
        if (!Catalogos.ESTADOS_PROYECTO.containsKey(nuevo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido");
        }
        // Solo el administrador aprueba (PENDIENTE -> otro) o devuelve a PENDIENTE
        boolean esAdmin = "ADMINISTRADOR".equals(usuarioActual().getRol());
        if (!esAdmin && ("PENDIENTE".equals(nuevo) || "PENDIENTE".equalsIgnoreCase(proyecto.getEstado()))) {
            throw new AccessDeniedException("Solo el administrador puede aprobar proyectos");
        }
        proyecto.setEstado(nuevo);
        return proyectoService.guardar(proyecto, null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIDER', 'ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        Proyecto proyecto = buscar(id);
        exigirPropietarioOAdministrador(proyecto);
        proyectoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private Proyecto buscar(String id) {
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return proyecto;
    }

    private Usuarios usuarioActual() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuarios usuario) return usuario;
        throw new AccessDeniedException("No hay un usuario autenticado");
    }

    private void exigirPropietarioOAdministrador(Proyecto proyecto) {
        Usuarios usuario = usuarioActual();
        if (!"ADMINISTRADOR".equals(usuario.getRol()) && !usuario.getId().equals(proyecto.getIdLider())) {
            throw new AccessDeniedException("Solo el líder del proyecto puede modificarlo");
        }
    }
}
