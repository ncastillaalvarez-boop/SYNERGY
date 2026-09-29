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
    @GetMapping("/dashboard")
    public String dashboardLider(HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());

        Map<String, Integer> progresos = new HashMap<>();
        int totalTareas = 0;
        int tareasCompletadas = 0;
        int entregas = 0;
        for (Proyecto p : proyectos) {
            List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(p.getId());
            int hechas = (int) tareas.stream().filter(LiderController::completada).count();
            totalTareas += tareas.size();
            tareasCompletadas += hechas;
            progresos.put(p.getId(), tareas.isEmpty() ? 0 : Math.round(hechas * 100f / tareas.size()));
            for (Tarea t : tareas) {
                entregas += entregaTareaRepository.findByIdTarea(t.getId()).size();
            }
        }

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (String estado : List.of("ACTIVO", "PENDIENTE", "COMPLETADO", "CANCELADO")) {
            porEstado.put(estado, proyectos.stream().filter(p -> estado.equalsIgnoreCase(p.getEstado())).count());
        }

        model.addAttribute("usuario", lider);
        model.addAttribute("proyectos", proyectos);
        model.addAttribute("progresos", progresos);
        model.addAttribute("porEstado", porEstado);
        model.addAttribute("totalProyectos", proyectos.size());
        model.addAttribute("totalTareas", totalTareas);
        model.addAttribute("tareasCompletadas", tareasCompletadas);
        model.addAttribute("tareasPendientes", totalTareas - tareasCompletadas);
        model.addAttribute("porcentajeGlobal", totalTareas == 0 ? 0 : Math.round(tareasCompletadas * 100f / totalTareas));
        model.addAttribute("entregasRecibidas", entregas);
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
    public String guardarProyecto(@ModelAttribute Proyecto proyecto, HttpSession session, RedirectAttributes ra) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        if (proyecto.getFechaInicio() != null && proyecto.getFechaFin() != null
                && proyecto.getFechaFin().isBefore(proyecto.getFechaInicio())) {
            ra.addFlashAttribute("error", "La fecha de fin no puede ser anterior a la de inicio");
            return "redirect:/lider/proyecto/nuevo";
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
        if (proyecto.getFechaInicio() != null && proyecto.getFechaFin() != null
                && proyecto.getFechaFin().isBefore(proyecto.getFechaInicio())) {
            ra.addFlashAttribute("error", "La fecha de fin no puede ser anterior a la de inicio");
            return "detalle".equals(origen) ? "redirect:/lider/proyecto/" + id + "/detalle" : "redirect:/lider/proyecto/" + id + "/editar";
        }

        proyecto.setFechaActualizacion(LocalDateTime.now());
        proyectoRepository.save(proyecto);
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
    public String asignarTareas(HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }
        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());

        // Iniciales de los integrantes para los avatares de la tabla
        Map<String, String> iniciales = new HashMap<>();
        Map<String, String> nombres = new HashMap<>();
        for (Proyecto p : proyectos) {
            for (String uid : p.getParticipantes()) {
                if (!iniciales.containsKey(uid)) {
                    usuarioRepository.findById(uid).ifPresent(u -> {
                        iniciales.put(uid, u.getIniciales());
                        nombres.put(uid, u.getNombre());
                    });
                }
            }
        }

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("iniciales", iniciales);
        model.addAttribute("nombres", nombres);
        return "Lider_Asignar_Tareas";
    }

    @GetMapping("/proyecto/{id}/detalle")
    public String verDetalleProyecto(@PathVariable String id, HttpSession session, Model model) {
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
        model.addAttribute("usuario", lider);
        return "Lider_Detalle_Proyecto";
    }

    // =======================================================
    // USUARIOS INSCRITOS EN LOS PROYECTOS DEL LÍDER
    // =======================================================
    @GetMapping("/usuarios")
    public String usuariosInscritos(HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }

        Map<String, Usuarios> usuarios = new LinkedHashMap<>();
        Map<String, List<String>> proyectosPorUsuario = new HashMap<>();
        for (Proyecto p : proyectoService.obtenerProyectosDeLider(lider.getId())) {
            for (String uid : p.getParticipantes()) {
                if (uid.equals(lider.getId())) {
                    continue;
                }
                usuarioRepository.findById(uid).ifPresent(u -> {
                    usuarios.putIfAbsent(uid, u);
                    proyectosPorUsuario.computeIfAbsent(uid, k -> new ArrayList<>()).add(p.getNombre());
                });
            }
        }

        model.addAttribute("usuarios", new ArrayList<>(usuarios.values()));
        model.addAttribute("proyectosPorUsuario", proyectosPorUsuario);
        return "Lider_Usuarios_Proyecto";
    }

    // =======================================================
    // ENTREGAS RECIBIDAS
    // =======================================================
    @GetMapping("/tareas/recibidas")
    public String tareasRecibidas(HttpSession session, Model model) {
        Usuarios lider = validarLider(session);
        if (lider == null) {
            return "redirect:/login";
        }

        List<EntregaVista> vistas = new ArrayList<>();
        for (Proyecto p : proyectoService.obtenerProyectosDeLider(lider.getId())) {
            for (Tarea t : tareaService.obtenerTareasPorProyecto(p.getId())) {
                for (EntregaTarea e : entregaTareaRepository.findByIdTarea(t.getId())) {
                    Usuarios autor = e.getIdUsuario() == null ? null
                            : usuarioRepository.findById(e.getIdUsuario()).orElse(null);
                    vistas.add(new EntregaVista(
                            e.getId(), t.getTitulo(), t.getDescripcion(), p.getNombre(),
                            autor != null ? autor.getNombre() : "Usuario desconocido",
                            autor != null ? autor.getIniciales() : "?",
                            e.getNombreArchivo(), e.getFechaEntrega(), e.getEstado()));
                }
            }
        }
        vistas.sort(Comparator.comparing(EntregaVista::getFechaEntrega,
                Comparator.nullsLast(Comparator.reverseOrder())));

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
            ra.addFlashAttribute("mensaje", "REVISADO".equals(nuevo) ? "Entrega aprobada" : "Entrega rechazada");
        }
        return "redirect:/lider/tareas/recibidas";
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
