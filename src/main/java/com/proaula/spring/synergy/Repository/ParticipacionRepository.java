package com.proaula.spring.synergy.Repository;

import com.proaula.spring.synergy.Model.Participacion;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipacionRepository extends MongoRepository<Participacion, String> {

    // Verificar si ya está inscrito
    boolean existsByIdUsuarioAndIdProyecto(String idUsuario, String idProyecto);

    // Listar todos los participantes de un proyecto
    List<Participacion> findByIdProyecto(String idProyecto);

    // Buscar participación específica
    Participacion findByIdProyectoAndIdUsuario(String idProyecto, String idUsuario);

    // Buscar participación por usuario y proyecto (orden inversa)
    Participacion findByIdUsuarioAndIdProyecto(String idUsuario, String idProyecto);

    // Listar participaciones de un usuario
    List<Participacion> findByIdUsuario(String idUsuario);
}
