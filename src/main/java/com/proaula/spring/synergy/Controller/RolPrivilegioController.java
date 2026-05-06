package com.proaula.spring.synergy.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proaula.spring.synergy.Model.Privilegio;
import com.proaula.spring.synergy.Model.Rol;
import com.proaula.spring.synergy.Service.RolPrivilegioService;

@RestController
@RequestMapping("/api/admin/roles-privilegios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class RolPrivilegioController {

    @Autowired
    private RolPrivilegioService rolPrivilegioService;

    // ==================== ROLES ====================

    @GetMapping("/roles")
    public ResponseEntity<List<Rol>> obtenerTodosRoles() {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerTodosRoles());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<Rol> obtenerRolPorId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerRolPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/roles/nombre/{nombre}")
    public ResponseEntity<Rol> obtenerRolPorNombre(@PathVariable String nombre) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerRolPorNombre(nombre));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/roles")
    public ResponseEntity<Rol> crearRol(@RequestBody Rol rol) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(rolPrivilegioService.crearRol(rol));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<Rol> actualizarRol(@PathVariable String id, @RequestBody Rol rol) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.actualizarRol(id, rol));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<?> eliminarRol(@PathVariable String id) {
        try {
            rolPrivilegioService.eliminarRol(id);
            return ResponseEntity.ok("Rol eliminado");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/roles/{id}/privilegios/{privilegioId}")
    public ResponseEntity<?> agregarPrivilegioAlRol(@PathVariable String id, @PathVariable String privilegioId) {
        try {
            rolPrivilegioService.agregarPrivilegioAlRol(id, privilegioId);
            return ResponseEntity.ok("Privilegio agregado al rol");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/roles/{id}/privilegios/{privilegioId}")
    public ResponseEntity<?> removerPrivilegioDelRol(@PathVariable String id, @PathVariable String privilegioId) {
        try {
            rolPrivilegioService.removePrivilegioDelRol(id, privilegioId);
            return ResponseEntity.ok("Privilegio removido del rol");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/roles/activos")
    public ResponseEntity<List<Rol>> obtenerRolesActivos() {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerRolesActivos());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // ==================== PRIVILEGIOS ====================

    @GetMapping("/privilegios")
    public ResponseEntity<List<Privilegio>> obtenerTodosPrivilegios() {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerTodosPrivilegios());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/privilegios/{id}")
    public ResponseEntity<Privilegio> obtenerPrivilegioPorId(@PathVariable String id) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerPrivilegioPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/privilegios/nombre/{nombre}")
    public ResponseEntity<Privilegio> obtenerPrivilegioPorNombre(@PathVariable String nombre) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerPrivilegioPorNombre(nombre));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/privilegios")
    public ResponseEntity<Privilegio> crearPrivilegio(@RequestBody Privilegio privilegio) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(rolPrivilegioService.crearPrivilegio(privilegio));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/privilegios/{id}")
    public ResponseEntity<Privilegio> actualizarPrivilegio(@PathVariable String id, @RequestBody Privilegio privilegio) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.actualizarPrivilegio(id, privilegio));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/privilegios/{id}")
    public ResponseEntity<?> eliminarPrivilegio(@PathVariable String id) {
        try {
            rolPrivilegioService.eliminarPrivilegio(id);
            return ResponseEntity.ok("Privilegio eliminado");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/privilegios/recurso/{recurso}")
    public ResponseEntity<List<Privilegio>> obtenerPrivilegiosPorRecurso(@PathVariable String recurso) {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerPrivilegiosPorRecurso(recurso));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/privilegios/activos")
    public ResponseEntity<List<Privilegio>> obtenerPrivilegiosActivos() {
        try {
            return ResponseEntity.ok(rolPrivilegioService.obtenerPrivilegiosActivos());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
