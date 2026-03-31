package com.proaula.spring.synergy.Service;

import java.util.List;
import com.proaula.spring.synergy.Model.Tarea;

public interface TareaService {

    Tarea guardarTarea(Tarea tarea, List<String> usuariosIds);

    List<Tarea> obtenerTareasPorUsuario(String usuarioId);

    List<Tarea> obtenerTareasPorProyecto(String proyectoId);

    Tarea obtenerTareaPorId(String id);

    void cambiarEstado(String tareaId, String nuevoEstado);

    void marcarComoCompletada(String idTarea);

}
