package com.proaula.spring.synergy.Dto;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Filtros de búsqueda de proyectos que llegan por la URL:
 * ?q=playa&estado=ACTIVO&categoria=...&organizacion=...&ubicacion=...&orden=recientes&page=2
 * Spring lo llena solo con @ModelAttribute.
 */
public class FiltroProyectos {

    private String q;
    private String estado;
    private String categoria;
    private String organizacion;
    private String ubicacion;
    private String riesgo; // Alto, Medio, Bajo (solo administrador)
    private String orden = "recientes";
    private int page = 1;
    /** Rango de fechas de creación (solo panel del administrador). */
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate desde;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private java.time.LocalDate hasta;

    /** Si no se eligió estado, usa uno por defecto (p. ej. ACTIVO en "Explorar"). */
    public void estadoPorDefecto(String estadoInicial) {
        if (!tiene(estado)) {
            estado = estadoInicial;
        }
    }

    /** Hay filtros de texto o de clasificación (el estado se muestra aparte). */
    public boolean isActivo() {
        return tiene(q) || tiene(categoria) || tiene(organizacion) || tiene(ubicacion) || tiene(riesgo)
                || desde != null || hasta != null;
    }

    /** Parámetros actuales (sin la página) para construir los enlaces de paginación. */
    public MultiValueMap<String, String> comoParametros() {
        MultiValueMap<String, String> m = new LinkedMultiValueMap<>();
        agregar(m, "q", q);
        agregar(m, "estado", estado);
        agregar(m, "categoria", categoria);
        agregar(m, "organizacion", organizacion);
        agregar(m, "ubicacion", ubicacion);
        agregar(m, "riesgo", riesgo);
        if (desde != null) {
            m.add("desde", desde.toString());
        }
        if (hasta != null) {
            m.add("hasta", hasta.toString());
        }
        if (tiene(orden) && !"recientes".equals(orden)) {
            m.add("orden", orden);
        }
        return m;
    }

    static boolean tiene(String v) {
        return v != null && !v.isBlank();
    }

    private static void agregar(MultiValueMap<String, String> m, String clave, String valor) {
        if (tiene(valor)) {
            m.add(clave, valor.trim());
        }
    }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getOrganizacion() { return organizacion; }
    public void setOrganizacion(String organizacion) { this.organizacion = organizacion; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public String getRiesgo() { return riesgo; }
    public void setRiesgo(String riesgo) { this.riesgo = riesgo; }
    public String getOrden() { return orden; }
    public void setOrden(String orden) { this.orden = orden; }
    public java.time.LocalDate getDesde() { return desde; }
    public void setDesde(java.time.LocalDate desde) { this.desde = desde; }
    public java.time.LocalDate getHasta() { return hasta; }
    public void setHasta(java.time.LocalDate hasta) { this.hasta = hasta; }
    /** Si el rango viene al revés (desde > hasta), se intercambia. */
    public void ordenarFechas() {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            java.time.LocalDate t = desde;
            desde = hasta;
            hasta = t;
        }
    }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, page); }
}
