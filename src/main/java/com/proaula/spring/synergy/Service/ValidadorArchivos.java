package com.proaula.spring.synergy.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Revisa los archivos que suben los voluntarios como evidencia de una tarea.
 *
 *  - Solo extensiones permitidas (documentos, imágenes, hojas de cálculo, presentaciones y ZIP).
 *  - Tamaño máximo {@link #MAX_BYTES}.
 *  - El contenido debe coincidir con la extensión (los primeros bytes o "firma" del archivo):
 *    así no se puede subir, por ejemplo, un ejecutable renombrado como .pdf.
 *  - El nombre se limpia (sin rutas ni caracteres raros) antes de guardarlo.
 */
public final class ValidadorArchivos {

    public static final long MAX_BYTES = 10L * 1024 * 1024;

    public static final Set<String> EXTENSIONES = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp",
            "txt", "csv", "zip");

    /** Texto para los formularios (atributo accept y ayuda). */
    public static final String ACCEPT = ".pdf,.png,.jpg,.jpeg,.gif,.webp,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.odt,.ods,.odp,.txt,.csv,.zip";

    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF = {'G', 'I', 'F', '8'};
    private static final byte[] ZIP = {'P', 'K', 0x03, 0x04};           // docx, xlsx, pptx, odt... son ZIP
    private static final byte[] ZIP_VACIO = {'P', 'K', 0x05, 0x06};
    private static final byte[] OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0}; // doc, xls, ppt antiguos
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};             // webp

    private static final Map<String, byte[][]> FIRMAS = Map.ofEntries(
            Map.entry("pdf", new byte[][] {PDF}),
            Map.entry("png", new byte[][] {PNG}),
            Map.entry("jpg", new byte[][] {JPG}),
            Map.entry("jpeg", new byte[][] {JPG}),
            Map.entry("gif", new byte[][] {GIF}),
            Map.entry("webp", new byte[][] {RIFF}),
            Map.entry("docx", new byte[][] {ZIP, ZIP_VACIO}),
            Map.entry("xlsx", new byte[][] {ZIP, ZIP_VACIO}),
            Map.entry("pptx", new byte[][] {ZIP, ZIP_VACIO}),
            Map.entry("odt", new byte[][] {ZIP}),
            Map.entry("ods", new byte[][] {ZIP}),
            Map.entry("odp", new byte[][] {ZIP}),
            Map.entry("zip", new byte[][] {ZIP, ZIP_VACIO}),
            Map.entry("doc", new byte[][] {OLE}),
            Map.entry("xls", new byte[][] {OLE}),
            Map.entry("ppt", new byte[][] {OLE}));

    private ValidadorArchivos() {
    }

    public static String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    /** Nombre seguro: sin carpetas, sin caracteres de control y de máximo 120 caracteres. */
    public static String nombreSeguro(String original) {
        String nombre = original == null ? "" : original;
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        nombre = nombre.replaceAll("[\\p{Cntrl}\"<>|:*?]", "_").trim();
        if (nombre.isEmpty() || nombre.equals(".") || nombre.equals("..")) {
            nombre = "entrega";
        }
        if (nombre.length() > 120) {
            String ext = extension(nombre);
            nombre = nombre.substring(0, 120 - ext.length() - 1) + "." + ext;
        }
        return nombre;
    }

    /**
     * Lanza IllegalArgumentException con un mensaje para el usuario si el archivo no sirve.
     *
     * @param nombre nombre original del archivo
     * @param datos  contenido completo
     */
    public static void validar(String nombre, byte[] datos) {
        if (datos == null || datos.length == 0) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        if (datos.length > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el máximo de 10 MB.");
        }
        String ext = extension(nombre);
        if (!EXTENSIONES.contains(ext)) {
            throw new IllegalArgumentException("Tipo de archivo no permitido. Sube PDF, imágenes, documentos de Office, TXT, CSV o ZIP.");
        }
        byte[][] firmas = FIRMAS.get(ext);
        if (firmas != null && Arrays.stream(firmas).noneMatch(f -> empiezaCon(datos, f))) {
            throw new IllegalArgumentException("El contenido del archivo no corresponde a un ." + ext + ". Revisa que no esté dañado.");
        }
        if (("txt".equals(ext) || "csv".equals(ext)) && pareceBinario(datos)) {
            throw new IllegalArgumentException("El archivo ." + ext + " no parece ser de texto.");
        }
    }

    /** Tipo MIME a guardar (no se confía en el que manda el navegador). */
    public static String tipoMime(String nombre) {
        return switch (extension(nombre)) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "odt" -> "application/vnd.oasis.opendocument.text";
            case "ods" -> "application/vnd.oasis.opendocument.spreadsheet";
            case "odp" -> "application/vnd.oasis.opendocument.presentation";
            case "txt" -> "text/plain; charset=UTF-8";
            case "csv" -> "text/csv; charset=UTF-8";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    private static boolean empiezaCon(byte[] datos, byte[] firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (datos[i] != firma[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean pareceBinario(byte[] datos) {
        int n = Math.min(datos.length, 4096);
        for (int i = 0; i < n; i++) {
            if (datos[i] == 0) {
                return true;
            }
        }
        return false;
    }
}
