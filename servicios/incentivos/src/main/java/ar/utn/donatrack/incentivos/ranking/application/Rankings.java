package ar.utn.donatrack.incentivos.ranking.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;
import ar.utn.donatrack.incentivos.ranking.domain.CalculadorRanking;
import ar.utn.donatrack.incentivos.ranking.domain.RankingMensual;
import ar.utn.donatrack.incentivos.ranking.domain.RankingRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Generación (cierre de mes) y consulta de rankings mensuales. */
public class Rankings {

    private final RankingRepositorio rankings;
    private final Repositorios.InsigniaRepositorio insignias;
    private final Repositorios.PerfilRepositorio perfiles;
    private final Clock reloj;

    public Rankings(RankingRepositorio rankings, Repositorios.InsigniaRepositorio insignias,
                    Repositorios.PerfilRepositorio perfiles, Clock reloj) {
        this.rankings = rankings;
        this.insignias = insignias;
        this.perfiles = perfiles;
        this.reloj = reloj;
    }

    /** Calcula y PERSISTE el ranking del período (idempotente: reemplaza si ya existía). */
    public RankingMensual generar(YearMonth periodo) {
        RankingMensual r = calcular(periodo);
        rankings.guardar(r);
        return r;
    }

    /** Ranking en vivo del mes en curso (no se persiste). */
    public RankingMensual actual() {
        return calcular(YearMonth.now(reloj));
    }

    public Optional<Integer> posicionActual(UUID donanteId) {
        return actual().entradas().stream().filter(e -> e.donanteId().equals(donanteId))
                .map(RankingMensual.Entrada::posicion).findFirst();
    }

    public List<RankingMensual> historial() {
        return rankings.historial();
    }

    public RankingMensual de(YearMonth periodo) {
        return rankings.buscar(RankingMensual.idDe(periodo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Ranking", periodo));
    }

    /** Podio destacado en la aplicación: el último ranking cerrado (o el actual si no hay). */
    public RankingMensual destacado() {
        return rankings.historial().stream().findFirst().orElseGet(this::actual);
    }

    private RankingMensual calcular(YearMonth periodo) {
        ZoneId zona = reloj.getZone();
        Instant desde = periodo.atDay(1).atStartOfDay(zona).toInstant();
        Instant hasta = periodo.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant();
        List<InsigniaObtenida> delMes = insignias.obtenidasEntre(desde, hasta);
        Map<UUID, String> nombres = perfiles.buscarTodos(delMes.stream().map(InsigniaObtenida::donanteId).distinct().toList())
                .stream().collect(Collectors.toMap(PerfilDonante::donanteId, PerfilDonante::nombre, (a, b) -> a));
        return CalculadorRanking.calcular(periodo, delMes, nombres, reloj.instant());
    }
}
