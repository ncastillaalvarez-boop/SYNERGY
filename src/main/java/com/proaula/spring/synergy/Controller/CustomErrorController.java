package com.proaula.spring.synergy.Controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {

        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int statusCode = 500;

        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        model.addAttribute("status", statusCode);
        // Mensajes pensados para el usuario: no se muestran detalles internos del servidor
        model.addAttribute("message", switch (statusCode) {
            case 400 -> "La solicitud no es válida. Revisa los datos e intenta de nuevo.";
            case 401 -> "Necesitas iniciar sesión para ver esta página.";
            case 403 -> "No tienes permiso para ver esta página.";
            case 404 -> "La página que buscas no existe o fue movida.";
            case 413 -> "El archivo es demasiado grande (máximo 10 MB).";
            default -> "Ocurrió un error inesperado. Intenta de nuevo en unos minutos.";
        });
        if (statusCode >= 500) {
            Object excepcion = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
            org.slf4j.LoggerFactory.getLogger(CustomErrorController.class)
                    .error("Error {} en {}: {}", statusCode, request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                            excepcion != null ? excepcion : message);
        }

        // Redirigir según el tipo de error
        if (statusCode == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("status", 404);
        } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("status", 403);
        } else if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            model.addAttribute("status", 401);
        } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            model.addAttribute("status", 500);
        }

        // Plantilla: templates/Error.html
        return "Error";
    }
}