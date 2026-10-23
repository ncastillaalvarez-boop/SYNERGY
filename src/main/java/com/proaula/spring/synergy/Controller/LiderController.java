package com.proaula.spring.synergy.Controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Dto.EntregaVista;
import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.EntregaTareaRepository;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/lider")
public class LiderController {

    /** Estados que el líder puede poner. PENDIENTE lo pone/quita solo el administrador. */
    private static final Set<String> ESTADOS_PROYECTO = Set.of("ACTIVO", "COMPLETADO", "CANCELADO");
    private static final Set<String> ESTADOS_ENTREGA = Set.of("REVISADO", "RECHAZADO");

    @Autowired
    private TareaService tareaService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntregaTareaRepository entregaTareaRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private com.proaula.spring.synergy.Service.ValidacionService validacionService;

    @Autowired
    private com.proaula.spring.synergy.Service.NotificacionService notificaciones;

    @Autowired
    private com.proaula.spring.synergy.Service.RiesgoProyectoService riesgoService;

    @Autowired
    private com.proaula.spring.synergy.Service.VoluntariosService voluntariosService;

    @Autowired
    private com.proaula.spring.synergy.Service.ReporteService reporteService;

    // =======================================================
    // UTILIDADES
    // =======================================================
    private Usuarios validarLider(HttpSession session) {
        Usuarios usuario = SesionUtil.usuario(session);
        return SesionUtil.esLider(usuario) ? usuario : null;
    }

    /** Devuelve el proyecto solo si pertenece al líder. */
    private Proyecto proyectoDelLider(String id, Usuarios lider) {
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !Objects.equals(proyecto.getIdLider(), lider.getId())) {
            return null;
        }
        return proyecto;
    }

    private static boolean completada(Tarea t) {
        return "COMPLETADA".equalsIgnoreCase(t.getEstado());
    }

    /** Solo acepta valores de la lista (evita que se guarde cualquier texto desde el formulario). */
    static String deCatalogo(String valor, java.util.List<String> opciones) {
        return valor != null && opciones.contains(valor) ? valor : null;
    }

    private static String estadoValido(String estado) {
        String e = estado == null ? "" : estado.trim().toUpperCase();
        return ESTADOS_PROYECTO.contains(e) ? e : "ACTIVO";
    }

    // =======================================================
    // DASHBOARD
    // =======================================================
    /** Avance de un proyecto para la lista del panel. */
    public record AvanceProyecto(Proyecto proyecto, int total, int hechas, int porcentaje) {
        public boolean isSinTareas() {
            return total == 0;
        }
        /** Va atrasado si su avance está más de 15 puntos por debajo del tiempo transcurrido. */
        public boolean isAtrasado() {
            return total > 0 && "ACTIVO".equals(proyecto.getEstado()) && proyecto.getAvanceEsperadoPct() - porcentaje > 15;
        }
    }

    private static final int MAX_LISTA_PANEL = 8;

    @GetMapping("/dashboard")
    public String dashboardLider(@RequestParam(defaultValue = "activos") String vista,
                                 @RequestParam(defaultValue = "false") boolean todos,
                                 HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());
        Map<String, Proyecto> porId = new HashMap<>();
        proyectos.forEach(p -> porId.put(p.getId(), p));

        // Una sola consulta para todas las tareas de los proyectos (antes: una por proyecto)
        List<Tarea> tareas = new ArrayList<>();
        if (!porId.isEmpty()) {
            org.springframework.data.mongodb.core.query.Query qt = org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("idProyecto").in(porId.keySet()));
            qt.fields().include("titulo", "estado", "fechaEntrega", "idProyecto");
            tareas = mongoTemplate.find(qt, Tarea.class);
        }
        Map<String, int[]> conteo = new HashMap<>(); // idProyecto -> [total, hechas]
        List<Tarea> porVencer = new ArrayList<>();
        java.time.LocalDate hoy = java.time.LocalDate.now();
        int totalTareas = 0;
        int tareasCompletadas = 0;
        for (Tarea t : tareas) {
            if ("CANCELADA".equalsIgnoreCase(t.getEstado())) {
                continue;
            }
            int[] c = conteo.computeIfAbsent(t.getIdProyecto(), k -> new int[2]);
            c[0]++;
            totalTareas++;
            if (completada(t)) {
                c[1]++;
                tareasCompletadas++;
            } else if (t.getFechaEntrega() != null && !t.getFechaEntrega().isAfter(hoy.plusDays(7))
                    && porId.get(t.getIdProyecto()) != null && "ACTIVO".equals(porId.get(t.getIdProyecto()).getEstado())) {
                porVencer.add(t);
            }
        }

        // Entregas recibidas y por revisar: dos conteos (antes se descargaban todas con sus archivos)
        long entregas = 0;
        long porRevisar = 0;
        List<String> idsTareas = tareas.stream().map(Tarea::getId).toList();
        if (!idsTareas.isEmpty()) {
            var criterio = org.springframework.data.mongodb.core.query.Criteria.where("idTarea").in(idsTareas);
            entregas = mongoTemplate.count(org.springframework.data.mongodb.core.query.Query.query(criterio), EntregaTarea.class);
            porRevisar = mongoTemplate.count(org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("idTarea").in(idsTareas).and("estado").is("ENTREGADO")),
                    EntregaTarea.class);
        }

        Map<String, Integer> progresos = new HashMap<>();
        List<AvanceProyecto> avances = new ArrayList<>();
        for (Proyecto p : proyectos) {
            int[] c = conteo.getOrDefault(p.getId(), new int[2]);
            int pct = c[0] == 0 ? 0 : Math.round(c[1] * 100f / c[0]);
            progresos.put(p.getId(), pct);
            avances.add(new AvanceProyecto(p, c[0], c[1], pct));
        }

        // Lista "Avance por proyecto": por defecto solo activos, los de mayor riesgo primero
        String filtro = Set.of("activos", "completados", "todos").contains(vista) ? vista : "activos";
        List<AvanceProyecto> lista = avances.stream()
                .filter(a -> switch (filtro) {
                    case "activos" -> "ACTIVO".equals(a.proyecto().getEstado());
                    case "completados" -> "COMPLETADO".equals(a.proyecto().getEstado());
                    default -> true;
                })
                .sorted(Comparator.comparing((AvanceProyecto a) -> a.proyecto().getPuntajeRiesgo() == null ? -1.0 : a.proyecto().getPuntajeRiesgo())
                        .reversed()
                        .thenComparingInt(AvanceProyecto::porcentaje))
                .toList();
        model.addAttribute("vista", filtro);
        model.addAttribute("totalLista", lista.size());
        model.addAttribute("avances", todos || lista.size() <= MAX_LISTA_PANEL ? lista : lista.subList(0, MAX_LISTA_PANEL));
        model.addAttribute("verTodos", todos);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (String estado : List.of("ACTIVO", "PENDIENTE", "COMPLETADO", "CANCELADO")) {
            porEstado.put(estado, proyectos.stream().filter(p -> estado.equalsIgnoreCase(p.getEstado())).count());
        }
        Map<String, Long> riesgo = new LinkedHashMap<>();
        for (String nivel : List.of("Alto", "Medio", "Bajo")) {
            riesgo.put(nivel, proyectos.stream().filter(p -> "ACTIVO".equals(p.getEstado()) && nivel.equals(p.getNivelRiesgo())).count());
        }
        riesgo.put("Sin datos", proyectos.stream().filter(p -> "ACTIVO".equals(p.getEstado()) && p.getNivelRiesgo() == null).count());

        // Tarjetas "Mis proyectos": activos y pendientes primero, después el resto
        List<Proyecto> ordenados = new ArrayList<>(proyectos);
        ordenados.sort(Comparator.comparingInt((Proyecto p) -> switch (p.getEstado() == null ? "" : p.getEstado()) {
            case "ACTIVO" -> 0;
            case "PENDIENTE" -> 1;
            case "COMPLETADO" -> 2;
            default -> 3;
        }).thenComparing(p -> p.getNombre() == null ? "" : p.getNombre()));

        Map<String, String> nombresProyecto = new HashMap<>();
        proyectos.forEach(p -> nombresProyecto.put(p.getId(), p.getNombre()));

        model.addAttribute("usuario", lider);
        model.addAttribute("proyectos", ordenados);
        model.addAttribute("progresos", progresos);
        model.addAttribute("porEstado", porEstado);
        model.addAttribute("riesgoLider", riesgo);
        model.addAttribute("totalProyectos", proyectos.size());
        model.addAttribute("totalTareas", totalTareas);
        model.addAttribute("tareasCompletadas", tareasCompletadas);
        model.addAttribute("tareasPendientes", totalTareas - tareasCompletadas);
        model.addAttribute("porcentajeGlobal", totalTareas == 0 ? 0 : Math.round(tareasCompletadas * 100f / totalTareas));
        model.addAttribute("entregasRecibidas", entregas);
        model.addAttribute("entregasPorRevisar", porRevisar);
        porVencer.sort(Comparator.comparing(Tarea::getFechaEntrega));
        model.addAttribute("totalPorVencer", porVencer.size());
        model.addAttribute("tareasPorVencer", porVencer.size() > 6 ? porVencer.subList(0, 6) : porVencer);
        model.addAttribute("nombresProyecto", nombresProyecto);
        model.addAttribute("hoy", hoy);
        return "Lider_Proyecto_Dashboard";
    }

    // =======================================================
    // CREAR / EDITAR PROYECTO
    // =======================================================
    @GetMapping("/proyecto/nuevo")
    public String formularioNuevoProyecto(HttpSession session, Model model) {
        if (validarLider(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("proyecto", new Proyecto());
        return "Lider_Crear_Proyecto";
    }

    @PostMapping("/proyecto/guardar")
    public String guardarProyecto(@ModelAttribute Proyecto proyecto, HttpSession session, Model model, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        proyecto.setCategoria(deCatalogo(proyecto.getCategoria(), Catalogos.CATEGORIAS));
        String error = validacionService.validarProyecto(proyecto, null);
        if (error != null) {
            // Se vuelve al formulario con lo que escribió el líder (antes se perdía todo)
            model.addAttribute("proyecto", proyecto);
            model.addAttribute("error", error);
            return "Lider_Crear_Proyecto";
        }

        proyecto.setId(null);
        proyecto.setCodigo(null);
        proyecto.setIdLider(lider.getId());
        // Un proyecto nuevo del líder se publica activo (no tiene sentido crearlo "completado")
        proyecto.setEstado("ACTIVO");
        proyecto.setFechaCreacion(LocalDateTime.now());
        proyecto.setCategoria(deCatalogo(proyecto.getCategoria(), Catalogos.CATEGORIAS));
        proyecto.setTipoOrganizacion(deCatalogo(proyecto.getTipoOrganizacion(), Catalogos.ORGANIZACIONES));
        proyecto.setUbicacion(deCatalogo(proyecto.getUbicacion(), Catalogos.BARRIOS));
        proyecto.setAudiencia(deCatalogo(proyecto.getAudiencia(), Catalogos.AUDIENCIAS));
        Proyecto guardado = proyectoService.guardar(proyecto, null);

        historialService.registrarAccion("CREAR", "PROYECTO", "Proyecto creado: " + guardado.getNombre(), lider.getCorreo());
        ra.addFlashAttribute("mensaje", "Proyecto '" + guardado.getNombre() + "' creado");
        return "redirect:/lider/dashboard";
    }

    @GetMapping("/proyecto/{id}/editar")
    public String editarProyecto(@PathVariable String id, HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/lider/dashboard";
        }
        model.addAttribute("proyecto", proyecto);
        return "Lider_Editar_Proyecto";
    }

    /**
     * Actualiza SOLO los campos enviados. Así el formulario de "Guardar fechas"
     * del detalle no borra el nombre ni la descripción.
     */
    @PostMapping("/proyecto/{id}/actualizar")
    public String actualizarProyecto(@PathVariable String id,
                                     @ModelAttribute("datos") Proyecto datos,
                                     @RequestParam(required = false) String origen,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/lider/dashboard";
        }

        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            proyecto.setNombre(datos.getNombre().trim());
        }
        if (datos.getDescripcion() != null && !datos.getDescripcion().isBlank()) {
            proyecto.setDescripcion(datos.getDescripcion().trim());
        }
        if (datos.getFechaInicio() != null) {
            proyecto.setFechaInicio(datos.getFechaInicio());
        }
        if (datos.getFechaFin() != null) {
            proyecto.setFechaFin(datos.getFechaFin());
        }
        // El constructor de Proyecto pone "ACTIVO" por defecto: solo se cambia si viene del formulario de edición.
        // Un proyecto PENDIENTE no lo puede activar el propio líder (lo aprueba el administrador).
        if ("editar".equals(origen) && datos.getEstado() != null
                && !"PENDIENTE".equalsIgnoreCase(proyecto.getEstado())) {
            proyecto.setEstado(estadoValido(datos.getEstado()));
        }
        if ("editar".equals(origen)) {
            proyecto.setCategoria(deCatalogo(datos.getCategoria(), Catalogos.CATEGORIAS));
            proyecto.setTipoOrganizacion(deCatalogo(datos.getTipoOrganizacion(), Catalogos.ORGANIZACIONES));
            proyecto.setUbicacion(deCatalogo(datos.getUbicacion(), Catalogos.BARRIOS));
            proyecto.setAudiencia(deCatalogo(datos.getAudiencia(), Catalogos.AUDIENCIAS));
        }
        String nombreAnterior = proyectoRepository.findById(id).map(Proyecto::getNombre).orElse(null);
        String error;
        if ("editar".equals(origen)) {
            // Formulario completo: todas las reglas (incluida la categoría, que algunos proyectos antiguos no tienen)
            error = com.proaula.spring.synergy.Service.ValidacionService.validarCampos(proyecto);
            boolean cambioNombre = !com.proaula.spring.synergy.Service.ValidacionService.normalizar(proyecto.getNombre())
                    .equals(com.proaula.spring.synergy.Service.ValidacionService.normalizar(nombreAnterior));
            if (error == null && cambioNombre && validacionService.buscarDuplicado(proyecto.getNombre(), id) != null) {
                error = validacionService.validarProyecto(proyecto, id);
            }
        } else {
            // "Guardar fechas" del detalle: solo se revisan las fechas
            error = proyecto.getFechaInicio() != null && proyecto.getFechaFin() != null
                    && proyecto.getFechaFin().isBefore(proyecto.getFechaInicio())
                    ? "La fecha de fin no puede ser anterior a la de inicio" : null;
        }
        if (error != null) {
            ra.addFlashAttribute("error", error);
            return "detalle".equals(origen) ? "redirect:/lider/proyecto/" + id + "/detalle" : "redirect:/lider/proyecto/" + id + "/editar";
        }

        proyecto.setFechaActualizacion(LocalDateTime.now());
        proyectoRepository.save(proyecto);
        // Cambiar fechas o estado cambia el riesgo
        riesgoService.recalcularProyecto(proyecto.getId());
        historialService.registrarAccion("EDITAR", "PROYECTO", "Proyecto actualizado: " + proyecto.getNombre(), lider.getCorreo());
        ra.addFlashAttribute("mensaje", "Cambios guardados");

        return "detalle".equals(origen) ? "redirect:/lider/proyecto/" + id + "/detalle" : "redirect:/lider/dashboard";
    }

    @PostMapping("/proyecto/{id}/eliminar")
    public String eliminarProyecto(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoDelLider(id, lider);
        if (proyecto != null) {
            proyectoService.eliminar(id);
            historialService.registrarAccion("ELIMINAR", "PROYECTO", "Proyecto eliminado: " + proyecto.getNombre(), lider.getCorreo());
            ra.addFlashAttribute("mensaje", "Proyecto eliminado correctamente");
        }
        return "redirect:/lider/dashboard";
    }

    /** Marca el proyecto como COMPLETADO (antes se llamaba "aceptar"). */
    @PostMapping({"/proyecto/{id}/completar", "/proyecto/{id}/aceptar"})
    public String completarProyecto(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoDelLider(id, lider);
        if (proyecto != null) {
            proyecto.setEstado("COMPLETADO");
            proyecto.setFechaActualizacion(LocalDateTime.now());
            proyectoRepository.save(proyecto);
            historialService.registrarAccion("EDITAR", "PROYECTO", "Proyecto completado: " + proyecto.getNombre(), lider.getCorreo());
            ra.addFlashAttribute("mensaje", "Proyecto marcado como completado");
        }
        return "redirect:/lider/tareas/asignar";
    }

    // =======================================================
    // PROYECTOS Y TAREAS
    // =======================================================
    @GetMapping("/tareas/asignar")
    public String asignarTareas(@RequestParam(required = false) String q, HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());
        // Búsqueda (barra lateral): por nombre, código, categoría o barrio, sin importar tildes
        String texto = q == null ? "" : normalizar(q.trim());
        if (!texto.isEmpty()) {
            proyectos = proyectos.stream().filter(p -> normalizar(p.getNombre()).contains(texto)
                    || normalizar(p.getCodigo()).contains(texto) || normalizar(p.getCategoria()).contains(texto)
                    || normalizar(p.getUbicacion()).contains(texto)).toList();
        }
        model.addAttribute("q", q);

        // Iniciales de los integrantes para los avatares de la tabla (una sola consulta)
        Map<String, String> iniciales = new HashMap<>();
        Map<String, String> nombres = new HashMap<>();
        java.util.Set<String> ids = new java.util.HashSet<>();
        proyectos.forEach(p -> ids.addAll(p.getParticipantes()));
        usuarioRepository.findAllById(ids).forEach(u -> {
            iniciales.put(u.getId(), u.getIniciales());
            nombres.put(u.getId(), u.getNombre());
        });

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("iniciales", iniciales);
        model.addAttribute("nombres", nombres);
        return "Lider_Asignar_Tareas";
    }

    @GetMapping("/proyecto/{id}/detalle")
    public String verDetalleProyecto(@PathVariable String id, @RequestParam(required = false) String mes,
                                     HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/lider/tareas/asignar";
        }

        List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(id);
        List<Usuarios> integrantes = new ArrayList<>();
        for (String uid : proyecto.getParticipantes()) {
            usuarioRepository.findById(uid).ifPresent(integrantes::add);
        }
        int hechas = (int) tareas.stream().filter(LiderController::completada).count();

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("tareas", tareas);
        model.addAttribute("integrantes", integrantes);
        model.addAttribute("progreso", tareas.isEmpty() ? 0 : Math.round(hechas * 100f / tareas.size()));
        model.addAttribute("tareasHechas", hechas);
        model.addAttribute("usuario", lider);

        // Tablero kanban: tareas por estado
        Map<String, List<Tarea>> tablero = new LinkedHashMap<>();
        for (String estado : List.of("PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA")) {
            tablero.put(estado, new ArrayList<>());
        }
        for (Tarea t : tareas) {
            String e = t.getEstado() == null ? "PENDIENTE" : t.getEstado().toUpperCase();
            tablero.computeIfAbsent(e, k -> new ArrayList<>()).add(t);
        }
        model.addAttribute("tablero", tablero);
        model.addAttribute("nombresEstado", Map.of("PENDIENTE", "Pendiente", "EN_PROGRESO", "En progreso",
                "COMPLETADA", "Completada", "CANCELADA", "Cancelada"));

        // Calendario de entregas del mes (por defecto el mes actual o el de inicio si aún no empieza)
        java.time.LocalDate referencia = proyecto.getFechaInicio() != null && proyecto.getFechaInicio().isAfter(java.time.LocalDate.now())
                ? proyecto.getFechaInicio() : java.time.LocalDate.now();
        model.addAttribute("calendario", com.proaula.spring.synergy.Dto.Calendario.de(
                com.proaula.spring.synergy.Dto.Calendario.leerMes(mes, referencia), tareas, Tarea::getFechaEntrega));
        model.addAttribute("diasSemana", com.proaula.spring.synergy.Dto.Calendario.diasSemana());

        // Voluntarios: inactivos y recomendados; historial del riesgo
        java.util.Set<String> activosIds = voluntariosService.activos(proyecto);
        model.addAttribute("activosIds", activosIds);
        model.addAttribute("inactivos", voluntariosService.inactivos(proyecto));
        model.addAttribute("recomendados", "ACTIVO".equals(proyecto.getEstado())
                ? voluntariosService.recomendados(proyecto, 6) : List.of());
        model.addAttribute("historialRiesgo", riesgoService.historial(proyecto.getId(), 70));
        model.addAttribute("hoy", java.time.LocalDate.now());
        return "Lider_Detalle_Proyecto";
    }

    // =======================================================
    // USUARIOS INSCRITOS EN LOS PROYECTOS DEL LÍDER
    // =======================================================
    @GetMapping("/usuarios")
    public String usuariosInscritos(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) String proyecto,
                                    @RequestParam(defaultValue = "1") int page,
                                    HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }

        List<Proyecto> proyectosLider = proyectoService.obtenerProyectosDeLider(lider.getId());
        String filtroProyecto = proyectosLider.stream().anyMatch(p -> p.getId().equals(proyecto)) ? proyecto : null;

        // id del voluntario -> nombres de los proyectos del líder en los que participa
        Map<String, List<String>> proyectosPorUsuario = new LinkedHashMap<>();
        for (Proyecto p : proyectosLider) {
            if (filtroProyecto != null && !p.getId().equals(filtroProyecto)) {
                continue;
            }
            for (String uid : p.getParticipantes()) {
                if (uid != null && !uid.equals(lider.getId())) {
                    proyectosPorUsuario.computeIfAbsent(uid, k -> new ArrayList<>()).add(p.getNombre());
                }
            }
        }

        // Una sola consulta para todos los voluntarios (antes era una por persona)
        String texto = normalizar(q);
        List<Usuarios> voluntarios = new ArrayList<>();
        usuarioRepository.findAllById(proyectosPorUsuario.keySet()).forEach(u -> {
            if (texto.isEmpty() || normalizar(u.getNombre()).contains(texto) || normalizar(u.getCorreo()).contains(texto)) {
                voluntarios.add(u);
            }
        });
        voluntarios.sort(java.util.Comparator.comparing(u -> normalizar(u.getNombre())));

        int total = voluntarios.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(total / (double) VOLUNTARIOS_POR_PAGINA));
        int pagina = Math.min(Math.max(page, 1), totalPaginas) - 1;
        int desde = pagina * VOLUNTARIOS_POR_PAGINA;
        List<Usuarios> visibles = voluntarios.subList(Math.min(desde, total), Math.min(desde + VOLUNTARIOS_POR_PAGINA, total));
        org.springframework.data.domain.Page<Usuarios> pageData = new org.springframework.data.domain.PageImpl<>(
                visibles, org.springframework.data.domain.PageRequest.of(pagina, VOLUNTARIOS_POR_PAGINA), total);

        org.springframework.util.LinkedMultiValueMap<String, String> parametros = new org.springframework.util.LinkedMultiValueMap<>();
        if (q != null && !q.isBlank()) {
            parametros.add("q", q.trim());
        }
        if (filtroProyecto != null) {
            parametros.add("proyecto", filtroProyecto);
        }

        model.addAttribute("usuarios", visibles);
        model.addAttribute("proyectosPorUsuario", proyectosPorUsuario);
        model.addAttribute("proyectosLider", proyectosLider);
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("proyectoFiltro", filtroProyecto);
        model.addAttribute("hayFiltros", !parametros.isEmpty());
        model.addAttribute("totalVoluntarios", total);
        model.addAttribute("paginacion", new com.proaula.spring.synergy.Dto.Paginacion(pageData, "/lider/usuarios", parametros));
        return "Lider_Usuarios_Proyecto";
    }

    private static final int VOLUNTARIOS_POR_PAGINA = 20;

    /** Minúsculas y sin tildes, para buscar "maria" y encontrar "María". */
    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return java.text.Normalizer.normalize(texto.trim().toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    // =======================================================
    // ENTREGAS RECIBIDAS
    // =======================================================
    @GetMapping("/tareas/recibidas")
    public String tareasRecibidas(@RequestParam(required = false) String estado, HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }

        // Antes: una consulta por tarea y por autor, y se traía el archivo completo de cada entrega
        List<EntregaVista> vistas = new ArrayList<>();
        Map<String, Tarea> tareasPorId = new HashMap<>();
        Map<String, Proyecto> proyectoDeTarea = new HashMap<>();
        for (Proyecto p : proyectoService.obtenerProyectosDeLider(lider.getId())) {
            for (Tarea t : tareaService.obtenerTareasPorProyecto(p.getId())) {
                tareasPorId.put(t.getId(), t);
                proyectoDeTarea.put(t.getId(), p);
            }
        }
        List<EntregaTarea> entregas = entregasSinArchivo(tareasPorId.keySet());
        Map<String, Usuarios> autores = new HashMap<>();
        usuarioRepository.findAllById(entregas.stream().map(EntregaTarea::getIdUsuario).filter(Objects::nonNull).distinct().toList())
                .forEach(u -> autores.put(u.getId(), u));
        for (EntregaTarea e : entregas) {
            Tarea t = tareasPorId.get(e.getIdTarea());
            Usuarios autor = e.getIdUsuario() == null ? null : autores.get(e.getIdUsuario());
            vistas.add(new EntregaVista(
                    e.getId(), t.getTitulo(), t.getDescripcion(), proyectoDeTarea.get(e.getIdTarea()).getNombre(),
                    autor != null ? autor.getNombre() : "Usuario desconocido",
                    autor != null ? autor.getIniciales() : "?",
                    e.getNombreArchivo(), e.getFechaEntrega(), e.getEstado()));
        }
        vistas.sort(Comparator.comparing(EntregaVista::getFechaEntrega,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // Pestañas: todas / por revisar / aprobadas / rechazadas
        Map<String, Long> conteoEstados = new LinkedHashMap<>();
        for (String e : List.of("ENTREGADO", "REVISADO", "RECHAZADO")) {
            conteoEstados.put(e, vistas.stream().filter(v -> e.equals(v.getEstado())).count());
        }
        String filtroEstado = estado != null && conteoEstados.containsKey(estado.toUpperCase()) ? estado.toUpperCase() : null;
        model.addAttribute("conteoEstados", conteoEstados);
        model.addAttribute("totalEntregas", vistas.size());
        model.addAttribute("estadoFiltro", filtroEstado);
        if (filtroEstado != null) {
            vistas = vistas.stream().filter(v -> filtroEstado.equals(v.getEstado())).toList();
        }
        model.addAttribute("entregas", vistas);
        return "Lider_Tareas_Recibidas";
    }

    /** Descarga el archivo de una entrega (solo el líder del proyecto). */
    @GetMapping("/entregas/{id}/archivo")
    public ResponseEntity<byte[]> descargarEntrega(@PathVariable String id, HttpSession session) {
        EntregaTarea entrega = entregaDelLider(id, validarLider(session));
        if (entrega == null || entrega.getDatos() == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType tipo;
        try {
            tipo = entrega.getTipoArchivo() != null ? MediaType.parseMediaType(entrega.getTipoArchivo())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (RuntimeException ex) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        String nombre = entrega.getNombreArchivo() != null ? entrega.getNombreArchivo() : "entrega";

        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombre, StandardCharsets.UTF_8).build().toString())
                .body(entrega.getDatos());
    }

    /** Marca una entrega como REVISADO (aprobada) o RECHAZADO. */
    @PostMapping("/entregas/{id}/revisar")
    public String revisarEntrega(@PathVariable String id,
                                 @RequestParam String estado,
                                 @RequestParam(required = false) String comentario,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        EntregaTarea entrega = entregaDelLider(id, lider);
        String nuevo = estado == null ? "" : estado.trim().toUpperCase();
        if (entrega != null && ESTADOS_ENTREGA.contains(nuevo)) {
            entrega.setEstado(nuevo);
            if (comentario != null && !comentario.isBlank()) {
                entrega.setComentariosRevisor(comentario.trim());
            }
            entregaTareaRepository.save(entrega);

            // Si se rechaza, la tarea vuelve a quedar pendiente para que se entregue de nuevo
            if ("RECHAZADO".equals(nuevo)) {
                tareaService.cambiarEstado(entrega.getIdTarea(), "PENDIENTE");
            }
            Tarea revisada = tareaService.obtenerTareaPorId(entrega.getIdTarea());
            String titulo = revisada != null ? revisada.getTitulo() : "tu tarea";
            notificaciones.notificar(entrega.getIdUsuario(), "ENTREGA",
                    ("REVISADO".equals(nuevo) ? "Entrega aprobada: " : "Entrega rechazada: ") + titulo,
                    entrega.getComentariosRevisor() != null && !entrega.getComentariosRevisor().isBlank()
                            ? "Comentario del líder: " + entrega.getComentariosRevisor()
                            : ("REVISADO".equals(nuevo) ? "¡Buen trabajo!" : "Revisa la tarea y vuelve a entregarla."),
                    "/tareas/buzon");
            ra.addFlashAttribute("mensaje", "REVISADO".equals(nuevo) ? "Entrega aprobada" : "Entrega rechazada");
        }
        return "redirect:/lider/tareas/recibidas";
    }

    // =======================================================
    // TABLERO, EQUIPO Y HERRAMIENTAS DEL PROYECTO
    // =======================================================

    private static final Set<String> ESTADOS_TAREA = Set.of("PENDIENTE", "EN_PROGRESO", "COMPLETADA", "CANCELADA");

    /**
     * Cambia el estado de una tarea desde el tablero kanban. Con fetch (cabecera X-Requested-With)
     * responde JSON; desde el formulario sin JavaScript vuelve al detalle.
     */
    @PostMapping("/tareas/{idTarea}/estado")
    public Object cambiarEstadoTarea(@PathVariable String idTarea, @RequestParam String estado, HttpSession session,
                                     jakarta.servlet.http.HttpServletRequest request, RedirectAttributes ra) {
        boolean ajax = "fetch".equals(request.getHeader("X-Requested-With"));
        Usuarios lider = validarLider(session);
        Tarea tarea = lider == null ? null : tareaService.obtenerTareaPorId(idTarea);
        Proyecto proyecto = tarea == null ? null : proyectoDelLider(tarea.getIdProyecto(), lider);
        String nuevo = estado == null ? "" : estado.trim().toUpperCase();
        if (proyecto == null || !ESTADOS_TAREA.contains(nuevo)) {
            return ajax ? ResponseEntity.status(403).body(Map.of("message", "No puedes cambiar esta tarea"))
                    : "redirect:/lider/dashboard";
        }
        tareaService.cambiarEstado(idTarea, nuevo);
        if (ajax) {
            return ResponseEntity.ok(Map.of("ok", true, "estado", nuevo));
        }
        ra.addFlashAttribute("mensaje", "Estado de la tarea actualizado");
        return "redirect:/lider/proyecto/" + proyecto.getId() + "/detalle#tablero";
    }

    /** Mensaje del líder a todo el equipo (llega como notificación a cada voluntario). */
    @PostMapping("/proyecto/{id}/mensaje")
    public String enviarMensaje(@PathVariable String id, @RequestParam String texto, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        Proyecto proyecto = lider == null ? null : proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/login";
        }
        String contenido = texto == null ? "" : texto.trim();
        if (contenido.isEmpty()) {
            ra.addFlashAttribute("error", "Escribe el mensaje antes de enviarlo");
        } else {
            List<String> destino = proyecto.getParticipantes().stream().filter(uid -> !uid.equals(lider.getId())).toList();
            int n = notificaciones.notificarVarios(destino, "MENSAJE", "Mensaje de " + proyecto.getNombre(),
                    contenido.length() > 500 ? contenido.substring(0, 500) : contenido, "/participacion/mis-proyectos");
            historialService.registrarAccion("CREAR", "PROYECTO", "Mensaje al equipo de " + proyecto.getNombre(), lider.getCorreo());
            ra.addFlashAttribute("mensaje", n == 0 ? "El proyecto aún no tiene voluntarios" : "Mensaje enviado a " + n + " voluntarios");
        }
        return "redirect:/lider/proyecto/" + id + "/detalle";
    }

    /** Recordatorio a los voluntarios inscritos que nunca han participado. */
    @PostMapping("/proyecto/{id}/recordar")
    public String recordarInactivos(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        Proyecto proyecto = lider == null ? null : proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/login";
        }
        List<String> ids = voluntariosService.inactivos(proyecto).stream().map(Usuarios::getId).toList();
        int n = notificaciones.notificarVarios(ids, "MENSAJE", "Te extrañamos en " + proyecto.getNombre(),
                "El equipo necesita tu ayuda: revisa tus tareas o deja un comentario en el proyecto.", "/tareas/buzon");
        ra.addFlashAttribute("mensaje", n == 0 ? "No hay voluntarios inactivos" : "Recordatorio enviado a " + n + " voluntarios");
        return "redirect:/lider/proyecto/" + id + "/detalle";
    }

    /** Invita a un voluntario recomendado (le llega una notificación con el enlace al proyecto). */
    @PostMapping("/proyecto/{id}/invitar/{usuarioId}")
    public String invitar(@PathVariable String id, @PathVariable String usuarioId, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        Proyecto proyecto = lider == null ? null : proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/login";
        }
        Usuarios invitado = usuarioRepository.findById(usuarioId).orElse(null);
        if (invitado == null || proyecto.getParticipantes().contains(usuarioId)) {
            ra.addFlashAttribute("error", "No se pudo invitar a ese voluntario");
        } else {
            String busqueda = proyecto.getCodigo() != null ? proyecto.getCodigo() : proyecto.getNombre();
            notificaciones.notificar(usuarioId, "PROYECTO", lider.getNombre() + " te invita a " + proyecto.getNombre(),
                    "Por tu experiencia en " + (proyecto.getCategoria() == null ? "proyectos similares" : proyecto.getCategoria().toLowerCase())
                            + ", creemos que te gustaría participar.",
                    "/proyectos/participar?q=" + java.net.URLEncoder.encode(busqueda, StandardCharsets.UTF_8));
            ra.addFlashAttribute("mensaje", "Invitación enviada a " + invitado.getNombre());
        }
        return "redirect:/lider/proyecto/" + id + "/detalle";
    }

    /**
     * Plantilla de proyecto: crea una copia (mismos datos y tareas, sin voluntarios) que empieza hoy
     * y dura lo mismo que el original. Las fechas de las tareas se corren igual.
     */
    @PostMapping("/proyecto/{id}/duplicar")
    public String duplicar(@PathVariable String id, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        Proyecto original = lider == null ? null : proyectoDelLider(id, lider);
        if (original == null) {
            return "redirect:/login";
        }
        java.time.LocalDate hoy = java.time.LocalDate.now();
        long desplazamiento = original.getFechaInicio() == null ? 0 : java.time.temporal.ChronoUnit.DAYS.between(original.getFechaInicio(), hoy);
        Proyecto copia = new Proyecto();
        String base = original.getNombre() + " (copia " + hoy.getYear() + ")";
        String nombre = base;
        for (int n = 2; validacionService.buscarDuplicado(nombre, null) != null && n < 50; n++) {
            nombre = base.replace(")", " #" + n + ")");
        }
        copia.setNombre(nombre.length() > com.proaula.spring.synergy.Service.ValidacionService.NOMBRE_MAX
                ? nombre.substring(0, com.proaula.spring.synergy.Service.ValidacionService.NOMBRE_MAX) : nombre);
        copia.setDescripcion(original.getDescripcion());
        copia.setCategoria(original.getCategoria());
        copia.setTipoOrganizacion(original.getTipoOrganizacion());
        copia.setUbicacion(original.getUbicacion());
        copia.setAudiencia(original.getAudiencia());
        copia.setFechaInicio(original.getFechaInicio() == null ? hoy : original.getFechaInicio().plusDays(desplazamiento));
        copia.setFechaFin(original.getFechaFin() == null ? hoy.plusMonths(3) : original.getFechaFin().plusDays(desplazamiento));
        copia.setIdLider(lider.getId());
        copia.setEstado("ACTIVO");
        copia.setFechaCreacion(LocalDateTime.now());
        Proyecto guardado = proyectoService.guardar(copia, null);

        int copiadas = 0;
        for (Tarea t : tareaService.obtenerTareasPorProyecto(original.getId())) {
            if ("CANCELADA".equalsIgnoreCase(t.getEstado())) {
                continue;
            }
            java.time.LocalDate entrega = t.getFechaEntrega() == null ? guardado.getFechaFin() : t.getFechaEntrega().plusDays(desplazamiento);
            Tarea nueva = new Tarea(t.getTitulo(), t.getDescripcion(), "PENDIENTE", entrega, guardado.getId());
            nueva = tareaService.guardarTarea(nueva, new ArrayList<>());
            guardado.addTarea(nueva.getId());
            copiadas++;
        }
        proyectoRepository.save(guardado);
        riesgoService.recalcularProyecto(guardado.getId());
        historialService.registrarAccion("CREAR", "PROYECTO", "Proyecto duplicado: " + guardado.getNombre(), lider.getCorreo());
        ra.addFlashAttribute("mensaje", "Se creó \"" + guardado.getNombre() + "\" con " + copiadas + " tareas. Revisa el nombre y las fechas.");
        return "redirect:/lider/proyecto/" + guardado.getId() + "/editar";
    }

    /** Reporte imprimible del proyecto (el navegador lo guarda como PDF con Ctrl+P). */
    @GetMapping("/proyecto/{id}/reporte")
    public String reporte(@PathVariable String id, HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        Proyecto proyecto = lider == null ? null : proyectoDelLider(id, lider);
        if (proyecto == null) {
            return "redirect:/login";
        }
        reporteService.preparar(proyecto, lider.getNombre(), model);
        model.addAttribute("volverA", "/lider/proyecto/" + proyecto.getId() + "/detalle");
        return "Reporte_Proyecto";
    }

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    /** Entregas de varias tareas SIN el campo "datos" (el archivo puede pesar varios MB). */
    private List<EntregaTarea> entregasSinArchivo(java.util.Collection<String> idsTareas) {
        if (idsTareas.isEmpty()) {
            return new ArrayList<>();
        }
        org.springframework.data.mongodb.core.query.Query q = org.springframework.data.mongodb.core.query.Query.query(
                org.springframework.data.mongodb.core.query.Criteria.where("idTarea").in(idsTareas));
        q.fields().exclude("datos");
        return mongoTemplate.find(q, EntregaTarea.class);
    }

    private EntregaTarea entregaDelLider(String idEntrega, Usuarios lider) {
        if (lider == null) {
            return null;
        }
        EntregaTarea entrega = entregaTareaRepository.findById(idEntrega).orElse(null);
        if (entrega == null) {
            return null;
        }
        Tarea tarea = tareaService.obtenerTareaPorId(entrega.getIdTarea());
        if (tarea == null || proyectoDelLider(tarea.getIdProyecto(), lider) == null) {
            return null;
        }
        return entrega;
    }
}
