package com.proaula.spring.synergy.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.proaula.spring.synergy.Model.Catalogos;
import com.proaula.spring.synergy.Security.SesionUtil;
import com.proaula.spring.synergy.Service.JerarquiaProyectoService;
import com.proaula.spring.synergy.Service.JerarquiaProyectoService.NodoProyecto;

import jakarta.servlet.http.HttpSession;

/**
 * Jerarquía de proyectos (árbol: organización → categoría → barrio → proyecto),
 * construida con los datos reales. Solo la ve el administrador.
 */
@Controller
@RequestMapping("/jerarquia")
public class JerarquiaController {

    private final JerarquiaProyectoService jerarquiaService;

    public JerarquiaController(JerarquiaProyectoService jerarquiaService) {
        this.jerarquiaService = jerarquiaService;
    }

    @GetMapping
    public String verJerarquia(@RequestParam(required = false) String categoria,
                               HttpSession session, Model model) {
        if (!SesionUtil.esAdmin(SesionUtil.usuario(session))) {
            return "redirect:/login";
        }
        String abierta = categoria != null && Catalogos.CATEGORIAS.contains(categoria) ? categoria : null;

        NodoProyecto raiz = jerarquiaService.construir(abierta);
        NodoProyecto nodoAbierto = abierta == null ? null : jerarquiaService.buscar(raiz, "cat:" + abierta);

        model.addAttribute("nodos", jerarquiaService.preorden(raiz));
        model.addAttribute("categoriaAbierta", abierta);
        model.addAttribute("ruta", nodoAbierto == null ? null : jerarquiaService.ruta(nodoAbierto));
        model.addAttribute("tareasCategoria", nodoAbierto == null ? 0 : jerarquiaService.contarTareas(nodoAbierto));
        return "jerarquia";
    }
}
