package com.proaula.spring.synergy.Dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Datos listos para pintar la barra de paginación (fragments/paginacion.html).
 * Las páginas se numeran desde 1 en la URL (?page=1) para que sean legibles.
 */
public class Paginacion {

    /** Un botón de la barra: número de página o puntos suspensivos. */
    public static class Enlace {
        private final String texto;
        private final String url;
        private final boolean actual;
        private final boolean separador;

        public Enlace(String texto, String url, boolean actual, boolean separador) {
            this.texto = texto;
            this.url = url;
            this.actual = actual;
            this.separador = separador;
        }

        public String getTexto() { return texto; }
        public String getUrl() { return url; }
        public boolean isActual() { return actual; }
        public boolean isSeparador() { return separador; }
    }

    private final int pagina;
    private final int totalPaginas;
    private final long totalElementos;
    private final int desde;
    private final int hasta;
    private final String urlAnterior;
    private final String urlSiguiente;
    private final List<Enlace> enlaces = new ArrayList<>();

    public Paginacion(Page<?> page, String rutaBase, MultiValueMap<String, String> parametros) {
        this.pagina = page.getNumber() + 1;
        this.totalPaginas = Math.max(1, page.getTotalPages());
        this.totalElementos = page.getTotalElements();
        this.desde = page.getTotalElements() == 0 ? 0 : (int) page.getPageable().getOffset() + 1;
        this.hasta = (int) Math.min(page.getTotalElements(), page.getPageable().getOffset() + page.getSize());

        this.urlAnterior = pagina > 1 ? url(rutaBase, parametros, pagina - 1) : null;
        this.urlSiguiente = pagina < totalPaginas ? url(rutaBase, parametros, pagina + 1) : null;

        // Ventana: 1 … (p-2) (p-1) p (p+1) (p+2) … N
        int inicio = Math.max(1, pagina - 2);
        int fin = Math.min(totalPaginas, pagina + 2);
        if (inicio > 1) {
            enlaces.add(new Enlace("1", url(rutaBase, parametros, 1), false, false));
            if (inicio > 2) {
                enlaces.add(new Enlace("…", null, false, true));
            }
        }
        for (int i = inicio; i <= fin; i++) {
            enlaces.add(new Enlace(String.valueOf(i), url(rutaBase, parametros, i), i == pagina, false));
        }
        if (fin < totalPaginas) {
            if (fin < totalPaginas - 1) {
                enlaces.add(new Enlace("…", null, false, true));
            }
            enlaces.add(new Enlace(String.valueOf(totalPaginas), url(rutaBase, parametros, totalPaginas), false, false));
        }
    }

    private static String url(String rutaBase, MultiValueMap<String, String> parametros, int pagina) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath(rutaBase).queryParams(parametros);
        if (pagina > 1) {
            b.replaceQueryParam("page", pagina);
        }
        return b.encode().build().toUriString();
    }

    public int getPagina() { return pagina; }
    public int getTotalPaginas() { return totalPaginas; }
    public long getTotalElementos() { return totalElementos; }
    public int getDesde() { return desde; }
    public int getHasta() { return hasta; }
    public String getUrlAnterior() { return urlAnterior; }
    public String getUrlSiguiente() { return urlSiguiente; }
    public List<Enlace> getEnlaces() { return enlaces; }
    public boolean isVarias() { return totalPaginas > 1; }
}
