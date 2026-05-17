package com.proaula.spring.synergy.Controller;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Service.UsuarioService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.ParticipacionService;

@Controller
@RequestMapping("/proyectos")
public class ProyectoWebController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ParticipacionService participacionService;

    // ==============================
    // DASHBOARD DE PROYECTOS
    // ==============================
    @GetMapping("/dashboard")
    public String dashboardProyectos(Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("usuario", usuario);

        return "Dashboard_Usuarios_Proyecto";
    }

    // ==============================
    // REGISTRO DE PROYECTOS
    // ==============================
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("proyecto", new Proyecto());
        model.addAttribute("proyectos", proyectoService.listar());
        return "registro_proyectos";
    }

    @PostMapping("/guardar")
public String guardarProyecto(
        @ModelAttribute Proyecto proyecto,
        @RequestParam("archivo") MultipartFile archivo,
        Principal principal,
        Model model) {

    if (principal == null) {
        return "redirect:/login";
    }

    // Asignar líder correctamente usando el Principal provisto por JWT
    Usuarios lider = usuarioService.buscarPorCorreo(principal.getName());
    proyecto.setIdLider(lider.getId());

    try {
        proyectoService.guardar(proyecto, archivo);
        model.addAttribute("mensajeExito", "Proyecto registrado correctamente");
    } catch (RuntimeException e) {
        model.addAttribute("error", e.getMessage());
    }

    model.addAttribute("proyecto", new Proyecto());
    model.addAttribute("proyectos", proyectoService.listar());

    return "registro_proyectos";
}


    // ==============================
    // LISTA GENERAL PARA INSCRIBIRSE
    // ==============================
    private static final int PAGE_SIZE = 12;

    @GetMapping("/participar")
    public String mostrarListaParticipacion(
            @RequestParam(defaultValue = "0") int page,
            Principal principal, Model model) {

        if (principal != null) {
            Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());
            if (usuario != null) {
                model.addAttribute("usuario", usuario);
                model.addAttribute("inscritosSet",
                        participacionService.obtenerIdsProyectosPorUsuario(usuario.getId()));
            }
        }

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        Page<Proyecto> pagina = proyectoService.listarPaginadoSinDatos(pageable);
        List<Proyecto> proyectos = pagina.getContent();

        Map<String, String> liderPorProyecto = new HashMap<>();
        for (Proyecto p : proyectos) {
            if (p.getIdLider() != null) {
                try {
                    Usuarios lider = usuarioService.obtenerUsuarioPorId(p.getIdLider());
                    liderPorProyecto.put(p.getId(), lider != null ? lider.getNombre() : "—");
                } catch (Exception e) {
                    liderPorProyecto.put(p.getId(), "—");
                }
            } else {
                liderPorProyecto.put(p.getId(), "—");
            }
        }

        model.addAttribute("proyectos", proyectos);
        model.addAttribute("liderPorProyecto", liderPorProyecto);
        model.addAttribute("currentPage", pagina.getNumber());
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("hasPrev", pagina.hasPrevious());
        model.addAttribute("hasNext", pagina.hasNext());
        model.addAttribute("returnUrl", "/dashboard");
        model.addAttribute("baseUrl", "/proyectos/participar");
        return "Participacion_Proyecto_Usuario";
    }

    // ==============================
    // INSCRIBIR A UN PROYECTO
    // ==============================
    @PostMapping("/participacion/inscribir/{proyectoId}/{usuarioId}")
    public String inscribir(@PathVariable String proyectoId,
            @PathVariable String usuarioId,
            RedirectAttributes redirectAttributes) {

        boolean inscrito = participacionService.inscribirUsuario(proyectoId, usuarioId);

        if (inscrito) {
            redirectAttributes.addFlashAttribute("popup", "success");
            redirectAttributes.addFlashAttribute("mensaje", "Te has inscrito exitosamente en el proyecto 🎉");
        } else {
            redirectAttributes.addFlashAttribute("popup", "yaInscrito");
            redirectAttributes.addFlashAttribute("mensaje", "Ya estás inscrito en este proyecto.");
        }

        return "redirect:/dashboard";
    }

    // ==============================
    // SALIR DEL PROYECTO
    // ==============================
    @PostMapping("/participacion/eliminar/{proyectoId}/{usuarioId}")
    public String salirDelProyecto(@PathVariable String proyectoId,
            @PathVariable String usuarioId,
            RedirectAttributes redirectAttributes) {

        participacionService.eliminarParticipacion(proyectoId, usuarioId);

        redirectAttributes.addFlashAttribute("popup", "success");
        redirectAttributes.addFlashAttribute("mensaje", "Has salido del proyecto.");

        return "redirect:/proyectos/dashboard";
    }

    // ==============================
    // MIS PROYECTOS (DEL LÍDER)
    // ==============================
    @GetMapping("/mis_proyectos")
    public String listarMisProyectos(Model model, Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        Usuarios usuario = usuarioService.buscarPorCorreo(principal.getName());

        if (usuario == null) {
            return "redirect:/login";
        }

        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(usuario.getId());

        model.addAttribute("proyectos", proyectos);

        return "Lider_MisProyectos";
    }

    @GetMapping("/mis_proyectos/{idUsuario}")
    public String listarMisProyectos(
            @PathVariable String idUsuario,
            Model model) {

        // Obtener usuario por ID
        Usuarios usuario = usuarioService.obtenerUsuarioPorId(idUsuario);

        // Proyectos donde el usuario es líder
        List<Proyecto> proyectos = proyectoService.obtenerProyectosDeLider(idUsuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectos);

        return "Dashboard_MisProyectos";
    }

    // ==============================
    // IMAGEN DE PROYECTO
    // ==============================
    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> verImagenProyecto(@PathVariable String id) {
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || proyecto.getDatos() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType tipo = MediaType.IMAGE_JPEG;
        if (proyecto.getTipoArchivo() != null) {
            try { tipo = MediaType.parseMediaType(proyecto.getTipoArchivo()); } catch (Exception ignored) {}
        }
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(tipo);
        return new ResponseEntity<>(proyecto.getDatos(), headers, HttpStatus.OK);
    }

}