package com.proaula.spring.synergy.Dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Calendario mensual (semanas de lunes a domingo) con las tareas de cada día.
 * Lo usan el detalle del proyecto (líder) y el panel del voluntario.
 */
public record Calendario(YearMonth mes, List<List<Dia>> semanas) {

    public record Dia(LocalDate fecha, boolean delMes, boolean hoy, List<Object> eventos) {
        public int getNumero() {
            return fecha.getDayOfMonth();
        }
    }

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    public String getTitulo() {
        String nombre = mes.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, ES);
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear();
    }

    public String getAnterior() {
        return mes.minusMonths(1).toString();
    }

    public String getSiguiente() {
        return mes.plusMonths(1).toString();
    }

    public static List<String> diasSemana() {
        return List.of("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");
    }

    /** "2026-10" -> YearMonth; si no se entiende, el mes actual. */
    public static YearMonth leerMes(String texto, LocalDate porDefecto) {
        try {
            return texto == null || texto.isBlank() ? YearMonth.from(porDefecto) : YearMonth.parse(texto.trim());
        } catch (RuntimeException e) {
            return YearMonth.from(porDefecto);
        }
    }

    public static <T> Calendario de(YearMonth mes, List<T> elementos, Function<T, LocalDate> fecha) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = mes.atDay(1);
        while (inicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            inicio = inicio.minusDays(1);
        }
        LocalDate fin = mes.atEndOfMonth();
        while (fin.getDayOfWeek() != DayOfWeek.SUNDAY) {
            fin = fin.plusDays(1);
        }
        List<List<Dia>> semanas = new ArrayList<>();
        List<Dia> semana = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            List<Object> delDia = new ArrayList<>();
            for (T e : elementos) {
                if (d.equals(fecha.apply(e))) {
                    delDia.add(e);
                }
            }
            semana.add(new Dia(d, YearMonth.from(d).equals(mes), d.equals(hoy), delDia));
            if (semana.size() == 7) {
                semanas.add(semana);
                semana = new ArrayList<>();
            }
        }
        return new Calendario(mes, semanas);
    }
}
