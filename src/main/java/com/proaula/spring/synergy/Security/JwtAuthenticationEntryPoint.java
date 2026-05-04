package com.proaula.spring.synergy.Security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String path = request.getRequestURI();

        // Si es una petición a la API → responder con 401 JSON
        if (path.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"No autorizado\",\"mensaje\":\"" + authException.getMessage() + "\"}"
            );
        } 
        // Si es una petición desde el navegador → redirigir al login
        else {
            response.sendRedirect("/login?sessionExpirada=true");
        }
    }
}