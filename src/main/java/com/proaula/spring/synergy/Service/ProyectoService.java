package com.proaula.spring.synergy.Service;

import com.proaula.spring.synergy.Model.Proyecto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProyectoService {

    List<Proyecto> listar();

    List<Proyecto> listarSinDatos();

    Page<Proyecto> listarPaginado(Pageable pageable);

    Page<Proyecto> listarPaginadoSinDatos(Pageable pageable);

    List<Proyecto> obtenerProyectosDeLiderSinDatos(String idLider);

    Proyecto guardar(Proyecto proyecto, MultipartFile archivo);

    Proyecto buscarPorId(String id);

    void eliminar(String id);

    List<Proyecto> obtenerProyectosDeLider(String idLider);

}
