package com.proaula.spring.synergy.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.proaula.spring.synergy.Service.ParticipacionService;
import com.proaula.spring.synergy.Service.UsuarioService;
import com.proaula.spring.synergy.Model.Participacion;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;

import java.util.List;

@RequestMapping("/participacion")
@CrossOrigin(origins = "*")
@Controller
public class ParticipacionController {

    @Autowired
    private ParticipacionService participacionService;

    @Autowired
    private UsuarioService usuarioService;

    // =====================================================
    //  VISTA GENERAL
    // =====================================================
    @GetMapping("/Participacion_Proyecto")
    public String mostrarVista() {
        return "Participacion_Proyecto";
    }

    // =====================================================
    //  VER PROYECTOS DONDE PARTICIPA EL USUARIO (CORREGIDO)
    // =====================================================
    @GetMapping("/mis-proyectos/{usuarioId}")
    public String mostrarProyectosUsuario(@PathVariable String usuarioId, Model model) {

        // Obtener los proyectos donde participa
        List<Proyecto> proyectos = participacionService.listarProyectosPorUsuario(usuarioId);

        // Obtener información completa del usuario
        Usuarios usuario = null;
        try {
            usuario = usuarioService.obtenerUsuarioPorId(usuarioId);
        } catch (Exception e) {}

        model.addAttribute("proyectosParticipando", proyectos);
        model.addAttribute("usuario", usuario);

        return "Participacion_Proyecto_Salir";
    }

    // =====================================================
    //  LISTAR PARTICIPANTES DE UN PROYECTO
    // =====================================================
    @GetMapping("/listar/{proyectoId}")
    public ResponseEntity<List<Participacion>> listar(@PathVariable String proyectoId) {
        return ResponseEntity.ok(participacionService.listarParticipantes(proyectoId));
    }

    // =====================================================
    //  VERIFICAR SI UN USUARIO YA ESTÁ INSCRITO
    // =====================================================
    @GetMapping("/inscrito/{proyectoId}/{usuarioId}")
    public ResponseEntity<Boolean> estaInscrito(
            @PathVariable String proyectoId,
            @PathVariable String usuarioId) {

        return ResponseEntity.ok(participacionService.yaInscrito(proyectoId, usuarioId));
    }

    // =====================================================
    //  ELIMINAR PARTICIPACIÓN
    // =====================================================
    @DeleteMapping("/eliminar/{proyectoId}/{usuarioId}")
    public ResponseEntity<?> eliminar(
            @PathVariable String proyectoId,
            @PathVariable String usuarioId) {

        participacionService.eliminarParticipacion(proyectoId, usuarioId);

        return ResponseEntity.ok("Participación eliminada");
    }
}