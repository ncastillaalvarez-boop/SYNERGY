package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.EntregaTarea;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.EntregaTareaService;
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;
import com.proaula.spring.synergy.Service.TareaService;


import java.security.Principal;

@Controller
@RequestMapping("/lider")
public class LiderController {

    @Autowired
    private TareaService tareaService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ParticipacionService participacionService;

    @Autowired
    private EntregaTareaService entregaTareaService;

    // =======================================================
    // VALIDAR QUE EL USUARIO SEA LÍDER
    // =======================================================
    private Usuarios validarLider(Principal principal) {
        if (principal == null) return null;
        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());

        if (usuario == null)
            return null;

        if (!"LIDER".equalsIgnoreCase(usuario.getRol()))
            return null;

        return usuario;
    }

    // =======================================================
    // DASHBOARD DEL LÍDER
    // =======================================================
    @GetMapping("/dashboard")
    public String dashboardLider(Principal principal, Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());

        model.addAttribute("usuario", lider);
        model.addAttribute("proyectos", proyectos);

        return "Lider_Proyecto_Dashboard";
    }

    // =======================================================
    // FORMULARIO CREAR PROYECTO
    // =======================================================
    @GetMapping("/proyecto/nuevo")
    public String formularioNuevoProyecto(Principal principal, Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        model.addAttribute("proyecto", new Proyecto());
        return "Lider_Crear_Proyecto";
    }

    // =======================================================
    // GUARDAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/guardar")
    public String guardarProyecto(@ModelAttribute Proyecto proyecto,
                                  @RequestParam(value = "archivo", required = false)
                                  MultipartFile archivo,
                                  Principal principal) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        proyecto.setIdLider(lider.getId());
        proyectoService.guardar(proyecto, archivo);

        return "redirect:/lider/dashboard";
    }

    // =======================================================
    // EDITAR PROYECTO
    // =======================================================
    @GetMapping("/proyecto/{id}/editar")
    public String editarProyecto(@PathVariable String id,
                                 Principal principal,
                                 Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !proyecto.getIdLider().equals(lider.getId()))
            return "redirect:/lider/dashboard";

        model.addAttribute("proyecto", proyecto);

        return "Lider_Editar_Proyecto";
    }

    // =======================================================
    // ACTUALIZAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/{id}/actualizar")
    public String actualizarProyecto(@PathVariable String id,
                                     @ModelAttribute Proyecto proyecto,
                                     Principal principal) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        proyecto.setId(id);
        proyecto.setIdLider(lider.getId());

        proyectoService.guardar(proyecto, null);

        return "redirect:/lider/dashboard";  // ← CORREGIDO
    }

    // =======================================================
    // ASIGNAR TAREAS
    // =======================================================

    @GetMapping("/tareas/asignar")
    public String asignarTareas(Principal principal, Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());
        model.addAttribute("proyectos", proyectos);

        java.util.Map<String, java.util.List<Usuarios>> participantesPorProyecto = new java.util.HashMap<>();
        for (Proyecto p : proyectos) {
            participantesPorProyecto.put(p.getId(), participacionService.listarUsuariosPorProyecto(p.getId()));
        }
        model.addAttribute("participantesPorProyecto", participantesPorProyecto);

        return "Lider_Asignar_Tareas";
    }

    // VER USUARIOS INSCRITOS AL PROYECTO
    @GetMapping("/usuarios")
    public String usuariosInscritos(Principal principal, Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());

        java.util.LinkedHashMap<String, Usuarios> vistos = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.List<String>> proyectosDeUsuario = new java.util.HashMap<>();

        for (Proyecto p : proyectos) {
            for (Usuarios u : participacionService.listarUsuariosPorProyecto(p.getId())) {
                vistos.putIfAbsent(u.getId(), u);
                proyectosDeUsuario
                    .computeIfAbsent(u.getId(), k -> new java.util.ArrayList<>())
                    .add(p.getNombre());
            }
        }

        model.addAttribute("usuarios", new java.util.ArrayList<>(vistos.values()));
        model.addAttribute("proyectosDeUsuario", proyectosDeUsuario);
        return "Lider_Usuarios_Proyecto";
    }

    // VER TAREAS RECIBIDAS
    @GetMapping("/tareas/recibidas")
    public String tareasRecibidas(Principal principal, Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(lider.getId());

        java.util.List<java.util.Map<String, Object>> entregas = new java.util.ArrayList<>();
        for (Proyecto proyecto : proyectos) {
            List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(proyecto.getId());
            for (Tarea tarea : tareas) {
                List<EntregaTarea> entregasTarea = entregaTareaService.obtenerEntregasPorTarea(tarea.getId());
                for (EntregaTarea entrega : entregasTarea) {
                    Usuarios usuario = entrega.getIdUsuario() != null
                            ? usuarioService.obtenerUsuarioPorId(entrega.getIdUsuario())
                            : null;
                    java.util.Map<String, Object> item = new java.util.HashMap<>();
                    item.put("entregaId",           entrega.getId());
                    item.put("titulo",              tarea.getTitulo());
                    item.put("proyectoNombre",      proyecto.getNombre());
                    item.put("usuarioNombre",       usuario != null ? usuario.getNombre() : "Desconocido");
                    item.put("descripcionEvidencia",entrega.getDescripcionEvidencia());
                    item.put("nombreArchivo",       entrega.getNombreArchivo());
                    item.put("tipoArchivo",         entrega.getTipoArchivo());
                    item.put("fechaEntrega",        entrega.getFechaEntrega());
                    entregas.add(item);
                }
            }
        }

        model.addAttribute("entregas", entregas);
        return "Lider_Tareas_Recibidas";
    }

     // =======================================================
    // ELIMINAR PROYECTO
    // =======================================================
    @PostMapping("/proyecto/{id}/eliminar")
     public String eliminarProyecto(@PathVariable String id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !proyecto.getIdLider().equals(lider.getId()))
            return "redirect:/lider/dashboard";

        proyectoService.eliminar(id);

        redirectAttributes.addFlashAttribute("popup", "success");
        redirectAttributes.addFlashAttribute("mensaje", "Proyecto eliminado correctamente.");

        return "redirect:/lider/dashboard";
    }

    // =======================================================
    // VER DETALLE DEL PROYECTO CON TAREAS
    // =======================================================
    @GetMapping("/proyecto/{id}/detalle")
     public String verDetalleProyecto(@PathVariable String id,
                                  Principal principal,
                                  Model model) {

        Usuarios lider = validarLider(principal);
        if (lider == null)
            return "redirect:/login";

        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !proyecto.getIdLider().equals(lider.getId()))
        return "redirect:/lider/tareas/asignar";

        // Tareas del proyecto
        List<Tarea> tareas = tareaService.obtenerTareasPorProyecto(id);

        // Integrantes desde la colección participaciones (fuente de verdad)
        List<Usuarios> integrantes = participacionService.listarUsuariosPorProyecto(id);

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("tareas", tareas);
        model.addAttribute("integrantes", integrantes);
        model.addAttribute("usuario", lider);

        return "Lider_Detalle_Proyecto";
    }

    @PostMapping("/proyecto/{id}/aceptar")
    public String aceptarProyecto(@PathVariable String id,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {

            Usuarios lider = validarLider(principal);
            if (lider == null)
                return "redirect:/login";

            Proyecto proyecto = proyectoService.buscarPorId(id);
            if (proyecto != null) {
                proyecto.setEstado("COMPLETADO");
                proyectoService.guardar(proyecto, null);
                redirectAttributes.addFlashAttribute("mensaje", "✅ Proyecto aceptado correctamente");
            }

            return "redirect:/lider/tareas/asignar";
    }



}
