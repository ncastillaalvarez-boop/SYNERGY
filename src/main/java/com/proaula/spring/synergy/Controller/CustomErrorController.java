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
        model.addAttribute("message", message != null ? message : "Error inesperado");

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

        return "error";
    }
}