package com.proaula.spring.synergy.Service.Impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.proaula.spring.synergy.Model.Tarea;
import com.proaula.spring.synergy.Repository.TareaRepository;
import com.proaula.spring.synergy.Service.TareaService;

@Service
public class TareaServiceImpl implements TareaService{

    @Autowired
    private TareaRepository tareaRepository;

    /** Recalcula el riesgo del proyecto cuando cambian sus tareas (@Lazy evita dependencias circulares). */
    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.proaula.spring.synergy.Service.RiesgoProyectoService riesgoService;

    /** Marca la fecha de cambio (el modelo de riesgo la usa como fecha de cumplimiento) y guarda. */
    private Tarea guardarYRecalcular(Tarea tarea) {
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        if (tarea.getFechaCreacion() == null) {
            tarea.setFechaCreacion(ahora);
        }
        tarea.setFechaActualizacion(ahora);
        Tarea guardada = tareaRepository.save(tarea);
        if (riesgoService != null) {
            riesgoService.recalcularProyecto(guardada.getIdProyecto());
        }
        return guardada;
    }

    public Tarea guardarTarea(Tarea tarea, List<String> usuariosIds) {
        if (usuariosIds != null && !usuariosIds.isEmpty()) {
            tarea.setUsuariosAsignados(usuariosIds);
        }
        return guardarYRecalcular(tarea);
    }

    public List<Tarea> obtenerTareasPorUsuario(String usuarioId) {
        return tareaRepository.findByUsuariosAsignadosContains(usuarioId);
    }

    public List<Tarea> obtenerTareasPorProyecto(String idProyecto) {
        return tareaRepository.findByIdProyecto(idProyecto);
    }

    public Tarea obtenerTareaPorId(String id) {
        return tareaRepository.findById(id).orElse(null);
    }

    public void cambiarEstado(String tareaId, String nuevoEstado) {
        Optional<Tarea> tareaOpt = tareaRepository.findById(tareaId);
        if (tareaOpt.isPresent()) {
            Tarea tarea = tareaOpt.get();
            tarea.setEstado(nuevoEstado);
            guardarYRecalcular(tarea);
        }
    }

    public void marcarComoCompletada(String idTarea) {
        Optional<Tarea> tareaOpt = tareaRepository.findById(idTarea);
        if (tareaOpt.isPresent()) {
            Tarea tarea = tareaOpt.get();
            tarea.setEstado("COMPLETADA");
            guardarYRecalcular(tarea);
        }
    }

    public List<Tarea> listar() {
        return tareaRepository.findAll();
    }

    public Tarea guardar(Tarea tarea) {
        return guardarYRecalcular(tarea);
    }

    public void eliminar(String id) {
        String idProyecto = tareaRepository.findById(id).map(Tarea::getIdProyecto).orElse(null);
        tareaRepository.deleteById(id);
        if (riesgoService != null) {
            riesgoService.recalcularProyecto(idProyecto);
        }
    }
}
