package com.proaula.spring.synergy.Controller;

import com.proaula.spring.synergy.Model.Usuarios;
import com.proaula.spring.synergy.Service.UsuarioService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class usuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public List<Usuarios> listar() {
        return usuarioService.obtenerTodosUsuarios();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuarios> obtener(@PathVariable String id) {
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorId(id);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Usuarios guardar(@RequestBody Usuarios usuario) {
        return usuarioService.guardarUsuario(usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        try {
            usuarioService.obtenerUsuarioPorId(id);
            usuarioService.eliminarUsuario(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    
}