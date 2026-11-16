package ar.utn.donatrack.incentivos.ranking.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Ranking persistido al cierre de cada mes (histórico consultable). */
public record RankingMensual(String id, int anio, int mes, List<Entrada> entradas, Instant generadoEn) {

    public static final int PODIO = 3;

    public record Entrada(int posicion, UUID donanteId, String nombre, int misionesCumplidas) {
    }

    public RankingMensual {
        entradas = List.copyOf(entradas);
    }

    public static String idDe(YearMonth periodo) {
        return periodo.toString();
    }

    public YearMonth periodo() {
        return YearMonth.of(anio, mes);
    }

    public List<Entrada> podio() {
        return entradas.stream().limit(PODIO).toList();
    }
}
