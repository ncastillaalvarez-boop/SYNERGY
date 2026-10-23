package com.proaula.spring.synergy.Controller;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Dto.FiltroProyectos;
import com.proaula.spring.synergy.Dto.Paginacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.BusquedaService;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.RiesgoProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Set<String> ROLES_VALIDOS = Set.of("ADMINISTRADOR", "LIDER", "PARTICIPANTE");
    private static final int POR_PAGINA = 15;
    private static final int POR_PAGINA_USUARIOS = 20;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private BusquedaService busquedaService;

    @Autowired
    private RiesgoProyectoService riesgoService;

    @Autowired
    private com.proaula.spring.synergy.Service.NotificacionService notificaciones;

    @Autowired
    private com.proaula.spring.synergy.Service.EstadisticasService estadisticas;

    private Usuarios validarAdmin(HttpSession session) {
        Usuarios usuario = SesionUtil.usuario(session);
        return SesionUtil.esAdmin(usuario) ? usuario : null;
    }

    // ---------------------------------------------------------
    // PROYECTOS
    // ---------------------------------------------------------
    @SuppressWarnings("unchecked")
    @GetMapping({"", "/", "/proyectos"})
    public String verProyectos(@ModelAttribute("filtro") FiltroProyectos filtro,
                               HttpSession session, Model model) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        filtro.ordenarFechas();
        // null = el administrador ve todos los estados, incluidos PENDIENTE y CANCELADO
        Page<Proyecto> pagina = busquedaService.buscarProyectos(filtro, null, POR_PAGINA);
        Set<String> idsLideres = pagina.getContent().stream()
                .map(Proyecto::getIdLider).filter(id -> id != null).collect(Collectors.toSet());

        model.addAttribute("usuario", admin);
        model.addAttribute("proyectos", pagina.getContent());
        model.addAttribute("paginacion", new Paginacion(pagina, "/admin/proyectos", filtro.comoParametros()));
        model.addAttribute("lideres", busquedaService.nombresDe(idsLideres));
        // Gráficas: una agregación de MongoDB por gráfica (antes 16 consultas), con el rango de fechas del filtro
        model.addAttribute("porEstado", estadisticas.proyectosPorEstado(filtro.getDesde(), filtro.getHasta()));
        model.addAttribute("volverProyectos", volverActual("/admin/proyectos", filtro.comoParametros(), filtro.getPage()));
        model.addAttribute("totalProyectos", busquedaService.contarProyectos(null));
        model.addAttribute("pendientes", busquedaService.contarProyectos("PENDIENTE"));
        model.addAttribute("activos", busquedaService.contarProyectos("ACTIVO"));
        model.addAttribute("totalUsuarios", busquedaService.contarUsuarios(null));
        // Modelo predictivo y gráficas
        java.util.Map<String, Long> porRiesgo = estadisticas.activosPorRiesgo();
        // Si ningún proyecto activo tiene nivel (base recién creada o el cálculo al arrancar falló),
        // se calcula ahora mismo, como máximo una vez cada 10 minutos.
        long conNivel = porRiesgo.getOrDefault("Alto", 0L) + porRiesgo.getOrDefault("Medio", 0L) + porRiesgo.getOrDefault("Bajo", 0L);
        if (conNivel == 0 && porRiesgo.getOrDefault("Sin datos", 0L) > 0
                && (riesgoService.getUltimoCalculo() == null
                    || riesgoService.getUltimoCalculo().isBefore(LocalDateTime.now().minusMinutes(10)))) {
            try {
                riesgoService.recalcularTodos();
                estadisticas.invalidar();
                porRiesgo = estadisticas.activosPorRiesgo();
            } catch (RuntimeException e) {
                // el mensaje se muestra abajo con riesgoError
            }
        }
        if (riesgoService.getUltimoError() != null && !model.containsAttribute("error")) {
            model.addAttribute("error", "Modelo de riesgo: " + riesgoService.getUltimoError());
        }
        model.addAttribute("porRiesgo", porRiesgo);
        model.addAttribute("riesgoAlto", porRiesgo.getOrDefault("Alto", 0L));
        java.util.Map<String, Long> porCategoria = estadisticas.proyectosPorCategoria(filtro.getDesde(), filtro.getHasta());
        model.addAttribute("porCategoria", porCategoria);
        model.addAttribute("maxCategoria", Math.max(1L, porCategoria.values().stream().mapToLong(Long::longValue).max().orElse(1L)));
        model.addAttribute("ordenesAdmin", ordenesAdmin());
        model.addAttribute("nivelesRiesgo", com.proaula.spring.synergy.Service.RiesgoCalculadora.NIVELES);

        // Tendencia de los últimos 12 meses
        java.util.List<com.proaula.spring.synergy.Service.EstadisticasService.Mes> tendencia = estadisticas.tendenciaMensual();
        model.addAttribute("tendencia", tendencia);
        model.addAttribute("maxTendencia", Math.max(1L, tendencia.stream()
                .mapToLong(m -> Math.max(m.creados(), Math.max(m.completados(), m.cancelados()))).max().orElse(1L)));

        // Propuestas esperando aprobación hace más de 3 días y tiempo promedio de aprobación
        model.addAttribute("pendientesAntiguos", estadisticas.pendientesAntiguos(DIAS_PENDIENTE, 5));
        model.addAttribute("totalPendientesAntiguos", estadisticas.contarPendientesAntiguos(DIAS_PENDIENTE));
        model.addAttribute("diasPendiente", DIAS_PENDIENTE);
        model.addAttribute("diasPromedioAprobacion", estadisticas.diasPromedioAprobacion());

        // Ranking: voluntarios con más entregas y líderes con más proyectos completados
        java.util.List<com.proaula.spring.synergy.Service.EstadisticasService.Puesto> topVol = estadisticas.topVoluntarios(5);
        java.util.List<com.proaula.spring.synergy.Service.EstadisticasService.Puesto> topLid = estadisticas.topLideres(5);
        java.util.Set<String> idsRanking = new java.util.HashSet<>();
        topVol.forEach(x -> idsRanking.add(x.idUsuario()));
        topLid.forEach(x -> idsRanking.add(x.idUsuario()));
        model.addAttribute("topVoluntarios", topVol);
        model.addAttribute("topLideres", topLid);
        model.addAttribute("nombresRanking", busquedaService.nombresDe(idsRanking));

        // Mapa: proyectos activos por barrio con coordenadas aproximadas
        java.util.List<java.util.Map<String, Object>> puntos = new java.util.ArrayList<>();
        estadisticas.activosPorBarrio().forEach((barrio, n) -> {
            double[] c = com.proaula.spring.synergy.Model.Catalogos.COORDENADAS_BARRIOS.get(barrio);
            if (c != null) {
                puntos.add(java.util.Map.of("barrio", barrio, "n", n, "lat", c[0], "lng", c[1]));
            }
        });
        model.addAttribute("puntosMapa", puntos);
        try {
            model.addAttribute("puntosMapaJson", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(puntos));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            model.addAttribute("puntosMapaJson", "[]");
        }
        model.addAttribute("totalPorEstado", Math.max(1L, ((java.util.Map<String, Long>) model.getAttribute("porEstado"))
                .values().stream().mapToLong(Long::longValue).sum()));
        model.addAttribute("modeloVersion", com.proaula.spring.synergy.Service.ModeloInfo.VERSION);
        return "Administrador_Dashboard";
    }

    /** Una propuesta lleva "mucho" esperando si pasan más de estos días sin aprobarse. */
    private static final int DIAS_PENDIENTE = 3;

    /** Órdenes del administrador: los del catálogo + prioridad por riesgo. */
    private static java.util.Map<String, String> ordenesAdmin() {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>(com.proaula.spring.synergy.Model.Catalogos.ORDENES_PROYECTO);
        m.put("riesgo", "Mayor riesgo primero");
        m.put("antiguos", "Más antiguos primero");
        return m;
    }

    // ---------------------------------------------------------
    // MODELO PREDICTIVO
    // ---------------------------------------------------------
    /** Dataset para entrenar el modelo en Google Colab (CSV) o en Weka (ARFF). */
    @GetMapping("/modelo/dataset.{formato}")
    public org.springframework.http.ResponseEntity<byte[]> descargarDataset(@PathVariable String formato, HttpSession session) {
        if (validarAdmin(session) == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .header(org.springframework.http.HttpHeaders.LOCATION, "/login").build();
        }
        boolean arff = "arff".equalsIgnoreCase(formato);
        var filas = riesgoService.construirDataset();
        String contenido = arff ? RiesgoProyectoService.aArff(filas) : RiesgoProyectoService.aCsv(filas);
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dataset_riesgo_synergy." + (arff ? "arff" : "csv") + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(arff ? "text/plain; charset=UTF-8" : "text/csv; charset=UTF-8"))
                .body(contenido.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/modelo/recalcular")
    public String recalcularRiesgo(HttpSession session, RedirectAttributes ra) {
        if (validarAdmin(session) == null) {
            return "redirect:/login";
        }
        try {
            int n = riesgoService.recalcularTodos();
            estadisticas.invalidar();
            java.util.Map<String, Long> r = estadisticas.activosPorRiesgo();
            ra.addFlashAttribute("mensaje", "Riesgo recalculado para " + n + " proyectos activos: Alto " + r.get("Alto")
                    + ", Medio " + r.get("Medio") + ", Bajo " + r.get("Bajo") + ", sin datos " + r.get("Sin datos") + ".");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "No se pudo recalcular el riesgo: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "redirect:/admin";
        }
        return "redirect:/admin/proyectos?estado=ACTIVO&orden=riesgo";
    }

    /** Ficha del modelo predictivo: versión, métricas, matriz de confusión y comparación de modelos. */
    @GetMapping("/modelo")
    public String verModelo(HttpSession session, Model model) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", admin);
        model.addAttribute("porRiesgo", estadisticas.activosPorRiesgo());
        model.addAttribute("ultimoCalculo", riesgoService.getUltimoCalculo());
        model.addAttribute("ultimoError", riesgoService.getUltimoError());
        // Ficha del modelo (Service/ModeloInfo): se pasa como mapa porque Thymeleaf no permite T(...) en las vistas
        java.util.Map<String, Object> ficha = new java.util.LinkedHashMap<>();
        ficha.put("version", com.proaula.spring.synergy.Service.ModeloInfo.VERSION);
        ficha.put("algoritmo", com.proaula.spring.synergy.Service.ModeloInfo.ALGORITMO);
        ficha.put("fecha", com.proaula.spring.synergy.Service.ModeloInfo.FECHA_ENTRENAMIENTO);
        ficha.put("dataset", com.proaula.spring.synergy.Service.ModeloInfo.DATASET);
        ficha.put("exportacion", com.proaula.spring.synergy.Service.ModeloInfo.EXPORTACION);
        ficha.put("f1cv", com.proaula.spring.synergy.Service.ModeloInfo.F1_MACRO_CV);
        ficha.put("f1cvDesv", com.proaula.spring.synergy.Service.ModeloInfo.F1_MACRO_CV_DESV);
        ficha.put("exactitud", com.proaula.spring.synergy.Service.ModeloInfo.EXACTITUD_PRUEBA);
        ficha.put("f1prueba", com.proaula.spring.synergy.Service.ModeloInfo.F1_MACRO_PRUEBA);
        model.addAttribute("ficha", ficha);
        java.util.List<String> clases = com.proaula.spring.synergy.Service.ModeloInfo.CLASES;
        java.util.List<java.util.Map<String, Object>> filas = new java.util.ArrayList<>();
        for (int i = 0; i < clases.size(); i++) {
            java.util.List<java.util.Map<String, Object>> celdas = new java.util.ArrayList<>();
            for (int j = 0; j < clases.size(); j++) {
                celdas.add(java.util.Map.of("n", com.proaula.spring.synergy.Service.ModeloInfo.MATRIZ_CONFUSION[i][j],
                        "pct", com.proaula.spring.synergy.Service.ModeloInfo.porcentajeCelda(i, j), "acierto", i == j));
            }
            filas.add(java.util.Map.of("clase", clases.get(i), "celdas", celdas,
                    "precision", Math.round(com.proaula.spring.synergy.Service.ModeloInfo.PRECISION[i] * 100),
                    "recall", Math.round(com.proaula.spring.synergy.Service.ModeloInfo.RECALL[i] * 100)));
        }
        model.addAttribute("clases", clases);
        model.addAttribute("matriz", filas);
        model.addAttribute("comparacion", com.proaula.spring.synergy.Service.ModeloInfo.COMPARACION);
        model.addAttribute("variables", com.proaula.spring.synergy.Service.RiesgoCalculadora.VARIABLES_NUMERICAS);
        return "Admin_Modelo";
    }

    // ---------------------------------------------------------
    // EXPORTAR (Excel abre los CSV directamente)
    // ---------------------------------------------------------
    private static final int MAX_EXPORTAR = 10000;
    private static final java.time.format.DateTimeFormatter FECHA = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Proyectos con los filtros actuales del panel (por ejemplo, solo los de riesgo alto). */
    @GetMapping("/exportar/proyectos.csv")
    public org.springframework.http.ResponseEntity<byte[]> exportarProyectos(@ModelAttribute("filtro") FiltroProyectos filtro,
                                                                             HttpSession session) {
        if (validarAdmin(session) == null) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }
        filtro.ordenarFechas();
        filtro.setPage(1);
        java.util.List<Proyecto> lista = busquedaService.buscarProyectos(filtro, null, MAX_EXPORTAR).getContent();
        java.util.Map<String, String> lideres = busquedaService.nombresDe(
                lista.stream().map(Proyecto::getIdLider).filter(java.util.Objects::nonNull).collect(Collectors.toSet()));
        java.util.List<Object[]> filas = new java.util.ArrayList<>();
        for (Proyecto p : lista) {
            filas.add(new Object[] {p.getCodigo(), p.getNombre(), p.getCategoria(), p.getUbicacion(), p.getEstado(),
                    lideres.getOrDefault(p.getIdLider(), ""), p.getFechaInicio(), p.getFechaFin(),
                    p.getParticipantes() == null ? 0 : p.getParticipantes().size(),
                    p.getNivelRiesgo(), p.getProbabilidadRiesgo() == null ? "" : Math.round(p.getProbabilidadRiesgo() * 100) + " %",
                    p.getRiesgoFactor(), p.getFechaEstimadaFin(),
                    p.getFechaCreacion() == null ? "" : p.getFechaCreacion().format(FECHA)});
        }
        String csv = com.proaula.spring.synergy.Service.ExportadorCsv.tabla(java.util.List.of("Código", "Nombre", "Categoría",
                "Barrio", "Estado", "Líder", "Inicio", "Fin", "Participantes", "Riesgo", "Probabilidad", "Lo que más influye",
                "Fin estimado", "Creado"), filas);
        return com.proaula.spring.synergy.Service.ExportadorCsv.descarga("proyectos_synergy_" + java.time.LocalDate.now() + ".csv", csv);
    }

    @GetMapping("/exportar/usuarios.csv")
    public org.springframework.http.ResponseEntity<byte[]> exportarUsuarios(@RequestParam(required = false) String q,
                                                                            @RequestParam(required = false) String rol,
                                                                            @RequestParam(required = false) String estado,
                                                                            HttpSession session) {
        if (validarAdmin(session) == null) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }
        java.util.List<Object[]> filas = new java.util.ArrayList<>();
        for (Usuarios u : busquedaService.buscarUsuarios(q, rol, estado, 1, MAX_EXPORTAR).getContent()) {
            filas.add(new Object[] {u.getNombre(), u.getCorreo(), u.getRol(), u.isEnabled() ? "Activo" : "Inactivo",
                    u.getProveedor() == null ? "LOCAL" : u.getProveedor(),
                    u.getProyectos() == null ? 0 : u.getProyectos().size(),
                    u.getFechaCreacion() == null ? "" : u.getFechaCreacion().format(FECHA),
                    u.getUltimoAcceso() == null ? "" : u.getUltimoAcceso().format(FECHA)});
        }
        String csv = com.proaula.spring.synergy.Service.ExportadorCsv.tabla(java.util.List.of("Nombre", "Correo", "Rol", "Estado",
                "Proveedor", "Proyectos", "Registro", "Último acceso"), filas);
        return com.proaula.spring.synergy.Service.ExportadorCsv.descarga("usuarios_synergy_" + java.time.LocalDate.now() + ".csv", csv);
    }

    @GetMapping("/proyecto/{id}")
    public String verProyecto(@PathVariable String id, HttpSession session, Model model) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null) {
            return "redirect:/admin/proyectos";
        }

        String nombreLider = "Sin asignar";
        if (proyecto.getIdLider() != null) {
            try {
                nombreLider = usuarioService.obtenerUsuarioPorId(proyecto.getIdLider()).getNombre();
            } catch (RuntimeException e) {
                nombreLider = "Líder no encontrado";
            }
        }

        model.addAttribute("usuario", admin);
        model.addAttribute("proyecto", proyecto);
        model.addAttribute("nombreLider", nombreLider);
        model.addAttribute("historialRiesgo", riesgoService.historial(proyecto.getId(), 70));
        return "Admin_Ver_Proyecto";
    }

    /** Aprueba un proyecto propuesto: pasa de PENDIENTE a ACTIVO. */
    @PostMapping("/proyecto/{id}/aceptar")
    public String aceptarProyecto(@PathVariable String id,
                                  @RequestParam(value = "volver", required = false) String volver,
                                  HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null && "PENDIENTE".equalsIgnoreCase(proyecto.getEstado())) {
            proyecto.setEstado("ACTIVO");
            proyecto.setFechaActualizacion(LocalDateTime.now());
            proyecto.setFechaAprobacion(LocalDateTime.now());
            proyectoRepository.save(proyecto);
            riesgoService.recalcularProyecto(proyecto.getId());
            estadisticas.invalidar();

            // Quien propuso el proyecto pasa a ser LÍDER al aprobarse (no antes)
            try {
                Usuarios proponente = usuarioService.obtenerUsuarioPorId(proyecto.getIdLider());
                if ("PARTICIPANTE".equalsIgnoreCase(proponente.getRol())) {
                    proponente.setRol("LIDER");
                    usuarioService.guardarUsuario(proponente);
                }
            } catch (RuntimeException e) {
                // proponente eliminado: el proyecto queda aprobado igual
            }

            historialService.registrarAccion("EDITAR", "PROYECTO", "Proyecto aprobado: " + proyecto.getNombre(), admin.getCorreo());
            notificaciones.notificar(proyecto.getIdLider(), "PROYECTO", "¡Tu proyecto fue aprobado!",
                    proyecto.getNombre() + " ya está publicado y puede recibir voluntarios.", "/lider/dashboard");
            ra.addFlashAttribute("mensaje", "Proyecto '" + proyecto.getNombre() + "' aprobado y publicado");
        }
        return "redirect:" + volverA(volver, "/admin/proyectos");
    }

    /** Rechaza una propuesta: pasa de PENDIENTE a CANCELADO (queda registro, no se borra). */
    @PostMapping("/proyecto/{id}/rechazar")
    public String rechazarProyecto(@PathVariable String id,
                                   @RequestParam(value = "volver", required = false) String volver,
                                   HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null && "PENDIENTE".equalsIgnoreCase(proyecto.getEstado())) {
            proyecto.setEstado("CANCELADO");
            proyecto.setFechaActualizacion(LocalDateTime.now());
            proyectoRepository.save(proyecto);
            historialService.registrarAccion("EDITAR", "PROYECTO", "Propuesta rechazada: " + proyecto.getNombre(), admin.getCorreo());
            notificaciones.notificar(proyecto.getIdLider(), "PROYECTO", "Tu propuesta no fue aprobada",
                    proyecto.getNombre() + ". Puedes ajustarla y proponerla de nuevo.", "/proyectos/registro");
            ra.addFlashAttribute("mensaje", "Propuesta '" + proyecto.getNombre() + "' rechazada");
        }
        return "redirect:" + volverA(volver, "/admin/proyectos");
    }

    @PostMapping("/proyecto/{id}/eliminar")
    public String eliminarProyecto(@PathVariable String id,
                                   @RequestParam(value = "volver", required = false) String volver,
                                   HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto != null) {
            proyectoService.eliminar(id);
            historialService.registrarAccion("ELIMINAR", "PROYECTO", "Proyecto eliminado: " + proyecto.getNombre(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Proyecto '" + proyecto.getNombre() + "' eliminado");
        }
        return "redirect:" + volverA(volver, "/admin/proyectos");
    }

    // ---------------------------------------------------------
    // USUARIOS
    // ---------------------------------------------------------
    @GetMapping("/usuarios")
    public String verUsuarios(@RequestParam(required = false) String q,
                              @RequestParam(required = false) String rol,
                              @RequestParam(required = false) String estado,
                              @RequestParam(defaultValue = "1") int page,
                              Model model, HttpSession session) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        Page<Usuarios> pagina = busquedaService.buscarUsuarios(q, rol, estado, page, POR_PAGINA_USUARIOS);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (q != null && !q.isBlank()) params.add("q", q.trim());
        if (rol != null && !rol.isBlank()) params.add("rol", rol);
        if (estado != null && !estado.isBlank()) params.add("estado", estado);

        model.addAttribute("usuario", admin);
        model.addAttribute("usuarios", pagina.getContent());
        model.addAttribute("paginacion", new Paginacion(pagina, "/admin/usuarios", params));
        model.addAttribute("q", q);
        model.addAttribute("rolFiltro", rol);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("hayFiltros", !params.isEmpty());
        model.addAttribute("volverUsuarios", volverActual("/admin/usuarios", params, page));
        model.addAttribute("totalUsuarios", busquedaService.contarUsuarios(null));
        model.addAttribute("totalAdmins", busquedaService.contarUsuarios("ADMINISTRADOR"));
        model.addAttribute("totalLideres", busquedaService.contarUsuarios("LIDER"));
        model.addAttribute("totalVoluntarios", busquedaService.contarUsuarios("PARTICIPANTE"));
        return "Admin_Usuarios_Dashboard";
    }

    /** URL de la página actual (con filtros) para volver después de una acción. */
    private static String volverActual(String ruta, MultiValueMap<String, String> params, int page) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(ruta).queryParams(params);
        if (page > 1) {
            b.queryParam("page", page);
        }
        return b.encode().build().toUriString();
    }

    /** Solo permite volver a rutas internas del panel de administración. */
    private static String volverA(String volver, String porDefecto) {
        if (volver != null && volver.startsWith("/admin/") && !volver.contains("//")) {
            return volver;
        }
        return porDefecto;
    }

    @PostMapping("/usuario/{id}/eliminar")
    public String eliminarUsuario(@PathVariable String id, @RequestParam(value = "volver", required = false) String volver,
                             HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        if (id.equals(admin.getId())) {
            ra.addFlashAttribute("error", "No puedes eliminar tu propia cuenta de administrador");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            usuarioService.eliminarUsuario(id);
            historialService.registrarAccion("ELIMINAR", "USUARIO", "Usuario eliminado: " + usuario.getCorreo(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Usuario '" + usuario.getNombre() + "' eliminado");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }

    @PostMapping("/usuario/{id}/cambiar-rol")
    public String cambiarRol(@PathVariable String id, @RequestParam String nuevoRol,
                             @RequestParam(value = "volver", required = false) String volver,
                             HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        String rol = nuevoRol == null ? "" : nuevoRol.trim().toUpperCase();
        if (!ROLES_VALIDOS.contains(rol)) {
            ra.addFlashAttribute("error", "Rol no válido");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }
        if (id.equals(admin.getId()) && !"ADMINISTRADOR".equals(rol)) {
            ra.addFlashAttribute("error", "No puedes quitarte el rol de administrador a ti mismo");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            // Un líder con proyectos no puede pasar a voluntario: sus proyectos quedarían sin gestor
            if ("PARTICIPANTE".equals(rol) && "LIDER".equalsIgnoreCase(usuario.getRol())
                    && !proyectoService.obtenerProyectosDeLider(id).isEmpty()) {
                ra.addFlashAttribute("error", "'" + usuario.getNombre() + "' lidera proyectos: no puede pasar a voluntario");
                return "redirect:" + volverA(volver, "/admin/usuarios");
            }
            usuario.setRol(rol);
            usuarioService.guardarUsuario(usuario);
            historialService.registrarAccion("EDITAR", "USUARIO", "Rol de " + usuario.getCorreo() + " cambiado a " + rol, admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Rol de '" + usuario.getNombre() + "' cambiado a " + rol);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }

    /** Activa o desactiva una cuenta (un usuario inactivo no puede iniciar sesión). */
    @PostMapping("/usuario/{id}/estado")
    public String cambiarEstado(@PathVariable String id, @RequestParam(value = "volver", required = false) String volver,
                             HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        if (id.equals(admin.getId())) {
            ra.addFlashAttribute("error", "No puedes desactivar tu propia cuenta");
            return "redirect:" + volverA(volver, "/admin/usuarios");
        }

        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            if (usuario.isEnabled()) {
                usuarioService.desactivarUsuario(id);
                historialService.registrarAccion("EDITAR", "USUARIO", "Cuenta desactivada: " + usuario.getCorreo(), admin.getCorreo());
                ra.addFlashAttribute("mensaje", "Cuenta de '" + usuario.getNombre() + "' desactivada");
            } else {
                usuarioService.activarUsuario(id);
                historialService.registrarAccion("EDITAR", "USUARIO", "Cuenta activada: " + usuario.getCorreo(), admin.getCorreo());
                ra.addFlashAttribute("mensaje", "Cuenta de '" + usuario.getNombre() + "' activada");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }

    /**
     * Recuperar contraseña sin correo electrónico: el administrador genera una contraseña temporal
     * y se la entrega al usuario, que luego la cambia en "Mi perfil".
     */
    @PostMapping("/usuario/{id}/restablecer-clave")
    public String restablecerClave(@PathVariable String id, @RequestParam(value = "volver", required = false) String volver,
                                   HttpSession session, RedirectAttributes ra) {
        Usuarios admin = validarAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            String temporal = claveTemporal();
            usuario.setContrasena(passwordEncoder.encode(temporal));
            usuario.setUltimaCambioContrasena(LocalDateTime.now());
            usuarioService.guardarUsuario(usuario);
            historialService.registrarAccion("EDITAR", "USUARIO", "Contraseña restablecida: " + usuario.getCorreo(), admin.getCorreo());
            ra.addFlashAttribute("mensaje", "Contraseña temporal de " + usuario.getCorreo() + ": " + temporal
                    + "  (cópiala ahora: no se volverá a mostrar)");
            ra.addFlashAttribute("claveTemporal", true);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "El usuario no existe");
        }
        return "redirect:" + volverA(volver, "/admin/usuarios");
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final java.security.SecureRandom AZAR = new java.security.SecureRandom();

    /** 12 caracteres que cumplen la política: mayúscula, minúscula, número y símbolo. */
    static String claveTemporal() {
        String mayus = "ABCDEFGHJKLMNPQRSTUVWXYZ", minus = "abcdefghijkmnpqrstuvwxyz", nums = "23456789", simb = "*#$%";
        String todos = mayus + minus + nums;
        StringBuilder sb = new StringBuilder();
        sb.append(mayus.charAt(AZAR.nextInt(mayus.length())));
        sb.append(minus.charAt(AZAR.nextInt(minus.length())));
        sb.append(nums.charAt(AZAR.nextInt(nums.length())));
        sb.append(simb.charAt(AZAR.nextInt(simb.length())));
        while (sb.length() < 12) {
            sb.append(todos.charAt(AZAR.nextInt(todos.length())));
        }
        // mezclar
        char[] c = sb.toString().toCharArray();
        for (int i = c.length - 1; i > 0; i--) {
            int j = AZAR.nextInt(i + 1);
            char t = c[i]; c[i] = c[j]; c[j] = t;
        }
        return new String(c);
    }
}
