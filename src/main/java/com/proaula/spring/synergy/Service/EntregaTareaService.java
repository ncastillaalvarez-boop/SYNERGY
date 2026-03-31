package com.proaula.spring.synergy.Service;

import com.proaula.spring.synergy.Model.EntregaTarea;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface EntregaTareaService {

    void guardarEntrega(String idTarea, MultipartFile archivo) throws IOException;

    List<EntregaTarea> obtenerEntregasPorTarea(String idTarea);

    boolean tareaYaEntregada(String idTarea);


}
