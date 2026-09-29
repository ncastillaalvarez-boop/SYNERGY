package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.ParticipacionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/participacion")
public class ParticipacionController {

    @Autowired
    private ParticipacionService participacionService;

    /** Proyectos en los que participa el usuario de la sesión (con opción de salir). */
    @GetMapping({"/mis-proyectos", "/mis-proyectos/{usuarioId}"})
    public String mostrarProyectosUsuario(HttpSession session, Model model) {
        Usuarios usuario = SesionUtil.usuario(session);
        if (usuario == null) {
            return "redirect:/login";
        }
        List<Proyecto> proyectos = participacionService.listarProyectosPorUsuario(usuario.getId());
        model.addAttribute("proyectosParticipando", proyectos);
        model.addAttribute("usuario", usuario);
        return "Participacion_Proyecto_Salir";
    }

    @GetMapping("/listar/{proyectoId}")
    public ResponseEntity<List<Participacion>> listar(@PathVariable String proyectoId, HttpSession session) {
        if (SesionUtil.usuario(session) == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(participacionService.listarParticipantes(proyectoId));
    }

    @GetMapping("/inscrito/{proyectoId}/{usuarioId}")
    public ResponseEntity<Boolean> estaInscrito(@PathVariable String proyectoId, @PathVariable String usuarioId) {
        return ResponseEntity.ok(participacionService.yaInscrito(proyectoId, usuarioId));
    }
}
