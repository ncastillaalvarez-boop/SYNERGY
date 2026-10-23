package com.proaula.spring.synergy.Controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints públicos de la API (sin token). */
@RestController
@RequestMapping("/api/public")
public class ApiPublicoController {

    /** La app móvil lo usa en "Servidor → Probar conexión". */
    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("ok", true, "servicio", "Synergy", "hora", LocalDateTime.now().toString());
    }
}
