package com.proaula.spring.synergy.Controller;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.proaula.spring.synergy.Dto.ComentarioVista;
import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Repository.ProyectoRepository;
import com.proaula.spring.synergy.Repository.UsuarioRepository;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.ComentarioService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.TareaService;

import jakarta.servlet.http.HttpSession;

/**
 * Buzón de comentarios de cada proyecto (líder, integrantes y administrador).
 */
@Controller
@RequestMapping("/proyectos")
public class ComentarioController {

    private static final int MAX_COMENTARIO = 2000;

    @Autowired
    private ComentarioService comentarioService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TareaService tareaService;

    private static boolean puedeVer(Proyecto proyecto, Usuarios usuario) {
        return usuario != null && (SesionUtil.esAdmin(usuario)
                || Objects.equals(proyecto.getIdLider(), usuario.getId())
                || proyecto.getParticipantes().contains(usuario.getId()));
    }

    /** A dónde vuelve el botón "Volver" según el rol. */
    private static String volverA(Proyecto proyecto, Usuarios usuario) {
        if (SesionUtil.esAdmin(usuario)) {
            return "/admin/proyecto/" + proyecto.getId();
        }
        if (Objects.equals(proyecto.getIdLider(), usuario.getId())) {
            return "/lider/proyecto/" + proyecto.getId() + "/detalle";
        }
        return "/participacion/mis-proyectos";
    }

    @GetMapping("/{id}/comentarios")
    public String verComentarios(@PathVariable String id, HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoService.buscarPorId(id);
        if (proyecto == null || !puedeVer(proyecto, usuario)) {
            return "redirect:" + SesionUtil.panelPorRol(usuario);
        }

        List<ComentarioVista> comentarios = comentarioService.listarPorProyecto(id).stream()
                .sorted(Comparator.comparing(Comentario::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> {
                    Usuarios autor = c.getIdUsuario() == null ? null
                            : usuarioRepository.findById(c.getIdUsuario()).orElse(null);
                    return new ComentarioVista(c.getId(),
                            autor != null ? autor.getNombre() : "Usuario eliminado",
                            autor != null ? autor.getIniciales() : "?",
                            c.getContenido(), c.getFecha(),
                            Objects.equals(c.getIdUsuario(), usuario.getId()));
                })
                .collect(Collectors.toList());

        // Tareas del proyecto asignadas al usuario (el líder y el admin ven todas)
        boolean gestiona = SesionUtil.esAdmin(usuario) || Objects.equals(proyecto.getIdLider(), usuario.getId());
        model.addAttribute("tareas", tareaService.obtenerTareasPorProyecto(id).stream()
                .filter(t -> gestiona || t.getUsuariosAsignados().contains(usuario.getId()))
                .collect(Collectors.toList()));

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("comentarios", comentarios);
        model.addAttribute("volverA", volverA(proyecto, usuario));
        return "Comentarios_Proyectos";
    }

    @PostMapping("/enviar-comentario")
    public String enviarComentario(@RequestParam String proyectoId,
                                   @RequestParam("comentario") String texto,
                                   HttpSession session,
                                   RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        if (proyecto == null || !puedeVer(proyecto, usuario)) {
            return "redirect:" + SesionUtil.panelPorRol(usuario);
        }

        String contenido = texto == null ? "" : texto.trim();
        if (contenido.isEmpty()) {
            ra.addFlashAttribute("error", "Escribe un comentario antes de enviarlo");
        } else {
            if (contenido.length() > MAX_COMENTARIO) {
                contenido = contenido.substring(0, MAX_COMENTARIO);
            }
            Comentario comentario = new Comentario(contenido, proyecto.getId(), usuario.getId());
            comentario.setFecha(LocalDateTime.now());
            Comentario guardado = comentarioService.guardarComentario(comentario);
            proyecto.addComentario(guardado.getId());
            proyectoRepository.save(proyecto);
            ra.addFlashAttribute("mensaje", "Comentario publicado");
        }
        return "redirect:/proyectos/" + proyectoId + "/comentarios";
    }

    /** Borra un comentario: su autor, el líder del proyecto o el administrador. */
    @PostMapping("/comentarios/eliminar/{id}")
    public String eliminarComentario(@PathVariable String id, @RequestParam String proyectoId,
                                     HttpSession session, RedirectAttributes ra) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        Comentario comentario = comentarioService.obtenerPorId(id);
        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        if (comentario != null && proyecto != null && Objects.equals(comentario.getIdProyecto(), proyectoId)
                && (Objects.equals(comentario.getIdUsuario(), usuario.getId())
                    || Objects.equals(proyecto.getIdLider(), usuario.getId())
                    || SesionUtil.esAdmin(usuario))) {
            comentarioService.eliminarComentario(id);
            proyecto.getComentarios().remove(id);
            proyectoRepository.save(proyecto);
            ra.addFlashAttribute("mensaje", "Comentario eliminado");
        }
        return "redirect:/proyectos/" + proyectoId + "/comentarios";
    }
}
