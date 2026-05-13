package com.proaula.spring.synergy.Controller;


import com.proaula.spring.synergy.Model.Comentario;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.ComentarioService;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/proyectos")
public class ComentarioController {

    @Autowired
    private ComentarioService comentarioService;

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private UsuarioService usuarioService;

    // VER comentarios de un proyecto

    @GetMapping("/{id}/comentarios")
    public String verComentarios(@PathVariable String id, Model model) {

        Proyecto proyecto = proyectoService.buscarPorId(id);

        if (proyecto == null) {
            return "redirect:/proyectos/mis-proyectos";
        }

        model.addAttribute("proyecto", proyecto);
        model.addAttribute("comentarios", comentarioService.listarPorProyecto(id));
        model.addAttribute("nuevoComentario", new Comentario());

        return "Comentarios_Proyectos"; // nombre EXACTO DEL HTML
    }

    // ENVIAR comentario

    @PostMapping("/enviar-comentario")
    public String enviarComentario(@ModelAttribute Comentario comentario,
            @RequestParam String proyectoId,
            @RequestParam String usuarioId) {

        Proyecto proyecto = proyectoService.buscarPorId(proyectoId);
        Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId);

        if (proyecto == null || usuario == null) {
            return "redirect:/proyectos/mis-proyectos";
        }

        comentario.setIdProyecto(proyecto.getId());
        comentario.setIdUsuario(usuario.getId());

        comentarioService.guardarComentario(comentario);

        return "redirect:/proyectos/" + proyectoId + "/comentarios";
    }

    // ELIMINAR comentario (opcional)

    @PostMapping("/comentarios/eliminar/{id}")
    public String eliminarComentario(@PathVariable String id,
            @RequestParam String proyectoId) {

        comentarioService.eliminarComentario(id);

        return "redirect:/proyectos/" + proyectoId + "/comentarios";
    }
}