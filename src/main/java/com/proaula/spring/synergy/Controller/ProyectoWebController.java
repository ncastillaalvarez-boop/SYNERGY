package com.proaula.spring.synergy.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.HistorialService;
import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/proyectos")
public class ProyectoWebController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ParticipacionService participacionService;

    @Autowired
    private HistorialService historialService;

    // ==============================
    // MENÚ DE PROYECTOS DEL PARTICIPANTE
    // ==============================
    @GetMapping("/dashboard")
    public String dashboardProyectos(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        model.addAttribute("usuario", usuario);
        return "Dashboard_Usuarios_Proyecto";
    }

    // ==============================
    // PROPONER UN PROYECTO (queda PENDIENTE hasta que el admin lo apruebe)
    // ==============================
    @GetMapping("/registro")
    public String mostrarRegistro(HttpSession session, Model model) {
        if (SesionUtil.usuario(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("proyecto", new Proyecto());
        return "registro_proyectos";
    }

    @PostMapping("/guardar")
    public String guardarProyecto(@ModelAttribute Proyecto proyecto,
                                  @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (proyecto.getFechaInicio() != null && proyecto.getFechaFin() != null
                && proyecto.getFechaFin().isBefore(proyecto.getFechaInicio())) {
            ra.addFlashAttribute("error", "La fecha de fin no puede ser anterior a la de inicio");
            return "redirect:/proyectos/registro";
        }

        proyecto.setId(null);
        proyecto.setCodigo(null);
        proyecto.setIdLider(usuario.getId());
        proyecto.setEstado("PENDIENTE");
        proyecto.setFechaCreacion(LocalDateTime.now());
        proyecto.setCategoria(LiderController.deCatalogo(proyecto.getCategoria(), Catalogos.CATEGORIAS));
        proyecto.setTipoOrganizacion(LiderController.deCatalogo(proyecto.getTipoOrganizacion(), Catalogos.ORGANIZACIONES));
        proyecto.setUbicacion(LiderController.deCatalogo(proyecto.getUbicacion(), Catalogos.BARRIOS));
        proyecto.setAudiencia(LiderController.deCatalogo(proyecto.getAudiencia(), Catalogos.AUDIENCIAS));

        try {
            // El proponente pasa a ser LÍDER cuando el administrador aprueba el proyecto
            Proyecto guardado = proyectoService.guardar(proyecto, archivo);
            historialService.registrarAccion("CREAR", "PROYECTO", "Proyecto propuesto: " + guardado.getNombre(), usuario.getCorreo());

            // Refrescar la sesión por si el rol cambió a LIDER
            SesionUtil.iniciar(session, usuarioService.obtenerUsuarioPorId(usuario.getId()));
            ra.addFlashAttribute("mensajeExito",
                    "Proyecto enviado. Un administrador lo revisará antes de publicarlo.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/proyectos/registro";
    }

    // ==============================
    // INSCRIBIRSE / SALIR
    // (la lista con filtros está en ExplorarController: /proyectos/participar)
    // ==============================
    @PostMapping("/participacion/inscribir/{proyectoId}/{usuarioId}")
    public String inscribir(@PathVariable String proyectoId,
                            @PathVariable String usuarioId,
                            @RequestParam(value = "volver", required = false) String volver,
                            HttpSession session,
                            RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        // Solo el propio usuario puede inscribirse
        if (usuario == null || !usuario.getId().equals(usuarioId)) {
            return "redirect:/login";
        }

        try {
            boolean inscrito = participacionService.inscribirUsuario(proyectoId, usuarioId);
            ra.addFlashAttribute(inscrito ? "mensaje" : "error",
                    inscrito ? "¡Te inscribiste en el proyecto!" : "Ya estabas inscrito en este proyecto");
            SesionUtil.iniciar(session, usuarioService.obtenerUsuarioPorId(usuarioId));
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        // Vuelve a la misma página y filtros desde donde se inscribió (solo rutas internas)
        if (volver != null && volver.startsWith("/proyectos/participar") && !volver.contains("//")) {
            return "redirect:" + volver;
        }
        return "redirect:/proyectos/participar";
    }

    @PostMapping("/participacion/eliminar/{proyectoId}/{usuarioId}")
    public String salirDelProyecto(@PathVariable String proyectoId,
                                   @PathVariable String usuarioId,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null || !usuario.getId().equals(usuarioId)) {
            return "redirect:/login";
        }
        participacionService.eliminarParticipacion(proyectoId, usuarioId);
        SesionUtil.iniciar(session, usuarioService.obtenerUsuarioPorId(usuarioId));
        ra.addFlashAttribute("mensaje", "Saliste del proyecto");
        return "redirect:/participacion/mis-proyectos";
    }

    // ==============================
    // PROYECTOS QUE EL USUARIO PROPUSO / LIDERA
    // ==============================
    @GetMapping({"/mis_proyectos", "/mis_proyectos/{idUsuario}"})
    public String listarMisProyectos(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        if (SesionUtil.esLider(usuario)) {
            return "redirect:/lider/dashboard";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("proyectos", proyectoService.obtenerProyectosDeLider(usuario.getId()));
        return "Dashboard_MisProyectos";
    }

    @GetMapping("/mis-proyectos")
    public String misProyectosParticipacion() {
        return "redirect:/participacion/mis-proyectos";
    }
}
