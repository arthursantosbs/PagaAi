package br.com.pagaai.domain;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.Map;

/** Nomes dos dias da semana em portugues, usados nos formularios e listagens. */
public final class DiasSemana {

    private static final Map<DayOfWeek, String> NOMES = new LinkedHashMap<>();

    static {
        NOMES.put(DayOfWeek.MONDAY, "Segunda-feira");
        NOMES.put(DayOfWeek.TUESDAY, "Terça-feira");
        NOMES.put(DayOfWeek.WEDNESDAY, "Quarta-feira");
        NOMES.put(DayOfWeek.THURSDAY, "Quinta-feira");
        NOMES.put(DayOfWeek.FRIDAY, "Sexta-feira");
        NOMES.put(DayOfWeek.SATURDAY, "Sábado");
        NOMES.put(DayOfWeek.SUNDAY, "Domingo");
    }

    private DiasSemana() {
    }

    public static String nome(DayOfWeek dia) {
        if (dia == null) {
            return "-";
        }
        return NOMES.get(dia);
    }

    public static Map<DayOfWeek, String> todos() {
        return NOMES;
    }
}
