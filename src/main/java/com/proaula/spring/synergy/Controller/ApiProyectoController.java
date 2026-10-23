package com.proaula.spring.synergy.Controller;

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
    private final com.proaula.spring.synergy.Service.ValidacionService validacionService;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public ApiProyectoController(ProyectoService proyectoService, ParticipacionService participacionService,
                                 com.proaula.spring.synergy.Service.RiesgoProyectoService riesgoService,
                                 com.proaula.spring.synergy.Service.ValidacionService validacionService,
                                 org.springframework.data.mongodb.core.MongoTemplate mongoTemplate) {
        this.proyectoService = proyectoService;
        this.participacionService = participacionService;
        this.riesgoService = riesgoService;
        this.validacionService = validacionService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Proyectos visibles para el usuario: los públicos (activos y completados) más los suyos.
     * El administrador ve todos. Antes cualquier usuario recibía también los pendientes y
     * cancelados de otros.
     *
     * Paginación opcional: ?page=1&size=20 (sin parámetros devuelve la lista completa, como antes).
     * El total va en la cabecera X-Total-Count.
     */
    @GetMapping
    public ResponseEntity<List<Proyecto>> listar(@org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
                                                 @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        Usuarios usuario = usuarioActual();
        org.springframework.data.mongodb.core.query.Query q = new org.springframework.data.mongodb.core.query.Query();
        if (!com.proaula.spring.synergy.Security.ApiAcceso.esAdmin(usuario)) {
            q.addCriteria(new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                    org.springframework.data.mongodb.core.query.Criteria.where("estado").in(Catalogos.ESTADOS_PUBLICOS),
                    org.springframework.data.mongodb.core.query.Criteria.where("idLider").is(usuario.getId()),
                    org.springframework.data.mongodb.core.query.Criteria.where("participantes").is(usuario.getId())));
        }
        long total = mongoTemplate.count(q, Proyecto.class);
        q.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fechaCreacion"));
        if (page != null || size != null) {
            int tam = Math.max(1, Math.min(size == null ? 20 : size, 100));
            int pag = Math.max(1, page == null ? 1 : page);
            q.skip((long) (pag - 1) * tam).limit(tam);
        }
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .body(mongoTemplate.find(q, Proyecto.class));
    }

    @GetMapping("/{id}")
    public Proyecto obtener(@PathVariable String id) {
        Proyecto p = buscar(id);
        com.proaula.spring.synergy.Security.ApiAcceso.exigirVer(p);
        return p;
    }

    /** Riesgo del proyecto según el modelo predictivo (para la app Flutter). */
    @GetMapping("/{id}/riesgo")
    public java.util.Map<String, Object> riesgo(@PathVariable String id) {
        Proyecto p = buscar(id);
        Usuarios usuario = usuarioActual();
        if (!com.proaula.spring.synergy.Security.ApiAcceso.gestiona(p, usuario)) {
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
        String error = validacionService.validarProyecto(proyecto, null);
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        proyecto.setId(null);
        proyecto.setCodigo(null);
        proyecto.setIdLider(usuario.getId());
        proyecto.setEstado("ACTIVO");
        proyecto.setFechaCreacion(java.time.LocalDateTime.now());
        proyecto.setTipoOrganizacion(LiderController.deCatalogo(proyecto.getTipoOrganizacion(), Catalogos.ORGANIZACIONES));
        proyecto.setUbicacion(LiderController.deCatalogo(proyecto.getUbicacion(), Catalogos.BARRIOS));
        proyecto.setAudiencia(LiderController.deCatalogo(proyecto.getAudiencia(), Catalogos.AUDIENCIAS));
        // No se aceptan del cliente: listas y resultado del modelo
        proyecto.setParticipantes(new java.util.ArrayList<>());
        proyecto.setTareas(new java.util.ArrayList<>());
        proyecto.setComentarios(new java.util.ArrayList<>());
        proyecto.setNivelRiesgo(null);
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
        String nombreAnterior = buscar(id).getNombre();
        if (cambios.getCategoria() != null) existente.setCategoria(LiderController.deCatalogo(cambios.getCategoria(), Catalogos.CATEGORIAS));
        if (cambios.getTipoOrganizacion() != null) existente.setTipoOrganizacion(LiderController.deCatalogo(cambios.getTipoOrganizacion(), Catalogos.ORGANIZACIONES));
        if (cambios.getUbicacion() != null) existente.setUbicacion(LiderController.deCatalogo(cambios.getUbicacion(), Catalogos.BARRIOS));
        if (cambios.getAudiencia() != null) existente.setAudiencia(LiderController.deCatalogo(cambios.getAudiencia(), Catalogos.AUDIENCIAS));
        String error = com.proaula.spring.synergy.Service.ValidacionService.validarCampos(existente);
        boolean cambioNombre = !com.proaula.spring.synergy.Service.ValidacionService.normalizar(existente.getNombre())
                .equals(com.proaula.spring.synergy.Service.ValidacionService.normalizar(nombreAnterior));
        if (error == null && cambioNombre) {
            error = validacionService.validarProyecto(existente, id);
        }
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        Proyecto guardado = proyectoService.guardar(existente, null);
        riesgoService.recalcularProyecto(id);
        return guardado;
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
        boolean esAdmin = com.proaula.spring.synergy.Security.ApiAcceso.esAdmin(usuarioActual());
        if (!esAdmin && ("PENDIENTE".equals(nuevo) || "PENDIENTE".equalsIgnoreCase(proyecto.getEstado()))) {
            throw new AccessDeniedException("Solo el administrador puede aprobar proyectos");
        }
        proyecto.setEstado(nuevo);
        Proyecto guardado = proyectoService.guardar(proyecto, null);
        riesgoService.recalcularProyecto(id);
        return guardado;
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
        return com.proaula.spring.synergy.Security.ApiAcceso.usuarioActual();
    }

    private void exigirPropietarioOAdministrador(Proyecto proyecto) {
        com.proaula.spring.synergy.Security.ApiAcceso.exigirGestion(proyecto, "Solo el líder del proyecto puede modificarlo");
    }
}
