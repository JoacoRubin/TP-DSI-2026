package ar.utn.donatrack.incentivos.ranking.domain;

import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Ranking de donantes más activos: misiones cumplidas durante el mes (sin importar la categoría). */
public final class CalculadorRanking {

    private CalculadorRanking() {
    }

    public static RankingMensual calcular(YearMonth periodo, List<InsigniaObtenida> delPeriodo, Map<UUID, String> nombres,
                                          Instant ahora) {
        Map<UUID, Long> porDonante = delPeriodo.stream()
                .collect(Collectors.groupingBy(InsigniaObtenida::donanteId, Collectors.counting()));
        List<Map.Entry<UUID, Long>> ordenado = porDonante.entrySet().stream()
                .sorted(Comparator.<Map.Entry<UUID, Long>>comparingLong(Map.Entry::getValue).reversed()
                        .thenComparing(e -> nombres.getOrDefault(e.getKey(), "")))
                .toList();
        List<RankingMensual.Entrada> entradas = new ArrayList<>();
        for (int i = 0; i < ordenado.size(); i++) {
            Map.Entry<UUID, Long> e = ordenado.get(i);
            entradas.add(new RankingMensual.Entrada(i + 1, e.getKey(), nombres.getOrDefault(e.getKey(), "Donante"),
                    e.getValue().intValue()));
        }
        return new RankingMensual(RankingMensual.idDe(periodo), periodo.getYear(), periodo.getMonthValue(), entradas, ahora);
    }
}
