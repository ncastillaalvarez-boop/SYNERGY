package com.proaula.spring.synergy.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Privilegio;
import com.proaula.spring.synergy.Model.Rol;
import com.proaula.spring.synergy.Repository.PrivilegioRepository;
import com.proaula.spring.synergy.Repository.RolRepository;

@Service
public class RolPrivilegioService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PrivilegioRepository privilegioRepository;

    // ==================== MÉTODOS PARA ROLES ====================

    public List<Rol> obtenerTodosRoles() {
        return rolRepository.findAll();
    }

    public Rol obtenerRolPorId(String id) {
        return rolRepository.findById(id).orElseThrow(() -> new RuntimeException("Rol no encontrado"));
    }

    public Rol obtenerRolPorNombre(String nombre) {
        return rolRepository.findByNombre(nombre)
            .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombre));
    }

    public Rol crearRol(Rol rol) {
        if (rolRepository.findByNombre(rol.getNombre()).isPresent()) {
            throw new RuntimeException("El rol ya existe: " + rol.getNombre());
        }
        return rolRepository.save(rol);
    }

    public Rol actualizarRol(String id, Rol rolActualizado) {
        Rol rolExistente = obtenerRolPorId(id);
        rolExistente.setNombre(rolActualizado.getNombre());
        rolExistente.setDescripcion(rolActualizado.getDescripcion());
        rolExistente.setPrivilegios(rolActualizado.getPrivilegios());
        rolExistente.setActivo(rolActualizado.getActivo());
        return rolRepository.save(rolExistente);
    }

    public void eliminarRol(String id) {
        rolRepository.deleteById(id);
    }

    public void agregarPrivilegioAlRol(String rolId, String privilegioId) {
        Rol rol = obtenerRolPorId(rolId);
        if (!rol.getPrivilegios().contains(privilegioId)) {
            rol.addPrivilegio(privilegioId);
            rolRepository.save(rol);
        }
    }

    public void removePrivilegioDelRol(String rolId, String privilegioId) {
        Rol rol = obtenerRolPorId(rolId);
        rol.removePrivilegio(privilegioId);
        rolRepository.save(rol);
    }

    public List<Rol> obtenerRolesActivos() {
        return rolRepository.findByActivo(true);
    }

    // ==================== MÉTODOS PARA PRIVILEGIOS ====================

    public List<Privilegio> obtenerTodosPrivilegios() {
        return privilegioRepository.findAll();
    }

    public Privilegio obtenerPrivilegioPorId(String id) {
        return privilegioRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Privilegio no encontrado"));
    }

    public Privilegio obtenerPrivilegioPorNombre(String nombre) {
        return privilegioRepository.findByNombre(nombre)
            .orElseThrow(() -> new RuntimeException("Privilegio no encontrado: " + nombre));
    }

    public Privilegio crearPrivilegio(Privilegio privilegio) {
        if (privilegioRepository.findByNombre(privilegio.getNombre()).isPresent()) {
            throw new RuntimeException("El privilegio ya existe: " + privilegio.getNombre());
        }
        return privilegioRepository.save(privilegio);
    }

    public Privilegio actualizarPrivilegio(String id, Privilegio privilegioActualizado) {
        Privilegio privilegioExistente = obtenerPrivilegioPorId(id);
        privilegioExistente.setNombre(privilegioActualizado.getNombre());
        privilegioExistente.setDescripcion(privilegioActualizado.getDescripcion());
        privilegioExistente.setRecurso(privilegioActualizado.getRecurso());
        privilegioExistente.setAccion(privilegioActualizado.getAccion());
        privilegioExistente.setActivo(privilegioActualizado.getActivo());
        return privilegioRepository.save(privilegioExistente);
    }

    public void eliminarPrivilegio(String id) {
        privilegioRepository.deleteById(id);
    }

    public List<Privilegio> obtenerPrivilegiosPorRecurso(String recurso) {
        return privilegioRepository.findByRecurso(recurso);
    }

    public List<Privilegio> obtenerPrivilegiosPorAccion(String accion) {
        return privilegioRepository.findByAccion(accion);
    }

    public List<Privilegio> obtenerPrivilegiosActivos() {
        return privilegioRepository.findByActivo(true);
    }

    public boolean usuarioTienePrivilegio(String idRol, String nombrePrivilegio) {
        if (idRol == null) return false;
        Optional<Rol> rolOpt = rolRepository.findById(idRol);
        if (!rolOpt.isPresent()) {
            return false;
        }
        
        Rol rol = rolOpt.get();
        List<String> privilegiosDelRol = rol.getPrivilegios();
        
        Optional<Privilegio> privilegioOpt = privilegioRepository.findByNombre(nombrePrivilegio);
        if (!privilegioOpt.isPresent()) {
            return false;
        }
        
        return privilegiosDelRol.contains(privilegioOpt.get().getId());
    }
}
