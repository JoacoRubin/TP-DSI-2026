package ar.utn.donatrack.incentivos.ranking.domain;

import java.util.List;
import java.util.Optional;

public interface RankingRepositorio {

    void guardar(RankingMensual ranking);

    Optional<RankingMensual> buscar(String id);

    List<RankingMensual> historial();
}
