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

    public Tarea guardarTarea(Tarea tarea, List<String> usuariosIds) {
        if (usuariosIds != null && !usuariosIds.isEmpty()) {
            tarea.setUsuariosAsignados(usuariosIds);
        }
        return tareaRepository.save(tarea);
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
            tareaRepository.save(tarea);
        }
    }

    public void marcarComoCompletada(String idTarea) {
        Optional<Tarea> tareaOpt = tareaRepository.findById(idTarea);
        if (tareaOpt.isPresent()) {
            Tarea tarea = tareaOpt.get();
            tarea.setEstado("Completada");
            tareaRepository.save(tarea);
        }
    }

    public List<Tarea> listar() {
        return tareaRepository.findAll();
    }

    public Tarea guardar(Tarea tarea) {
        return tareaRepository.save(tarea);
    }

    public void eliminar(String id) {
        tareaRepository.deleteById(id);
    }
}
