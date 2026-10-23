package com.proaula.spring.synergy.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.ProyectoService;
import com.proaula.spring.synergy.Service.RiesgoProyectoService;

import jakarta.servlet.http.HttpSession;

/** Simulador "¿qué pasa si...?" del panel de riesgo (líder del proyecto o administrador). */
@Controller
public class RiesgoController {

    private final ProyectoService proyectoService;
    private final RiesgoProyectoService riesgoService;

    public RiesgoController(ProyectoService proyectoService, RiesgoProyectoService riesgoService) {
        this.proyectoService = proyectoService;
        this.riesgoService = riesgoService;
    }

    @GetMapping("/riesgo/{id}/simular")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simular(@PathVariable String id,
                                                       @RequestParam(defaultValue = "0") int completar,
                                                       @RequestParam(defaultValue = "0") int activar,
                                                       @RequestParam(defaultValue = "0") int nuevos,
                                                       HttpSession session) {
        Usuarios u = SesionUtil.usuario(session);
        Proyecto p = id == null ? null : proyectoService.buscarPorId(id);
        if (u == null || p == null || !(SesionUtil.esAdmin(u) || u.getId().equals(p.getIdLider()))) {
            return ResponseEntity.status(403).body(Map.of("message", "Sin permiso"));
        }
        if (!"ACTIVO".equals(p.getEstado()) || p.getFechaInicio() == null || p.getFechaFin() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El simulador solo funciona con proyectos activos"));
        }
        return ResponseEntity.ok(riesgoService.simular(p, Math.min(completar, 500), Math.min(activar, 500), Math.min(nuevos, 50)));
    }
}
