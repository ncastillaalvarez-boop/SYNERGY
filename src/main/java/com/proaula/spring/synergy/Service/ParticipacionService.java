package com.proaula.spring.synergy.Service;

import java.util.List;
import com.proaula.spring.synergy.Model.Proyecto;
import com.proaula.spring.synergy.Model.Usuarios;

public interface ParticipacionService {

    boolean inscribirUsuario(String proyectoId, String usuarioId);

    List<com.proaula.spring.synergy.Model.Participacion> listarParticipantes(String proyectoId);

    boolean yaInscrito(String proyectoId, String usuarioId);

    void eliminarParticipacion(String proyectoId, String usuarioId);

    List<Proyecto> listarProyectosPorUsuario(String usuarioId);

    List<Usuarios> listarUsuariosPorProyecto(String proyectoId);

}
