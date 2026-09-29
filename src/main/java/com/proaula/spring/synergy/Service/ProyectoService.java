package com.proaula.spring.synergy.Service;

import com.proaula.spring.synergy.Model.Proyecto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProyectoService {

    List<Proyecto> listar();

    Proyecto guardar(Proyecto proyecto, MultipartFile archivo);

    Proyecto buscarPorId(String id);

    void eliminar(String id);

    List<Proyecto> obtenerProyectosDeLider(String idLider);

}
