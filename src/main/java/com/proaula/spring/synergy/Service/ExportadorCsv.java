package com.proaula.spring.synergy.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Archivos CSV que Excel abre bien en español: separador punto y coma (la coma es el separador
 * decimal en Colombia) y marca BOM para que respete las tildes.
 */
public final class ExportadorCsv {

    private ExportadorCsv() {
    }

    /** Escapa un valor: comillas si tiene ; " o saltos de línea, y evita fórmulas (=, +, -, @) en Excel. */
    public static String celda(Object valor) {
        if (valor == null) {
            return "";
        }
        String s = valor.toString();
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s; // inyección de fórmulas: Excel lo muestra como texto
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String fila(Object... valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(celda(valores[i]));
        }
        return sb.append("\r\n").toString();
    }

    public static String tabla(List<String> encabezados, List<Object[]> filas) {
        StringBuilder sb = new StringBuilder(fila(encabezados.toArray()));
        for (Object[] f : filas) {
            sb.append(fila(f));
        }
        return sb.toString();
    }

    /** Respuesta de descarga con BOM UTF-8. */
    public static ResponseEntity<byte[]> descarga(String nombreArchivo, String contenido) {
        byte[] cuerpo = ("﻿" + contenido).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(cuerpo);
    }
}
