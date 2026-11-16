package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.donaciones.asignacion.domain.ContextoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.MotorMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Servicio de aplicación compartido por los casos de uso de matchmaking. */
public class EvaluadorDonaciones {

    static final Duration TRIMESTRE = Duration.ofDays(90);

    private final MotorMatchmaking motor;
    private final NecesidadRepositorio necesidades;
    private final DonacionRepositorio donaciones;
    private final ResultadoMatchmakingRepositorio resultados;
    private final Clock reloj;

    public EvaluadorDonaciones(MotorMatchmaking motor, NecesidadRepositorio necesidades, DonacionRepositorio donaciones,
                               ResultadoMatchmakingRepositorio resultados, Clock reloj) {
        this.motor = motor;
        this.necesidades = necesidades;
        this.donaciones = donaciones;
        this.resultados = resultados;
        this.reloj = reloj;
    }

    /** Se calcula una vez por ejecución (no una consulta por par donación-necesidad). */
    ContextoMatchmaking contexto() {
        Map<UUID, Integer> recibidas = donaciones.entregadasPorEntidadDesde(reloj.instant().minus(TRIMESTRE));
        LocalDate hoy = LocalDate.now(reloj);
        return new ContextoMatchmaking() {
            public LocalDate hoy() { return hoy; }
            public int donacionesRecibidasEnTrimestre(UUID entidadId) { return recibidas.getOrDefault(entidadId, 0); }
        };
    }

    ResultadoMatchmaking evaluar(Donacion donacion, ContextoMatchmaking contexto) {
        ResultadoMatchmaking r = motor.evaluar(donacion, necesidades.activasDeSubcategoria(donacion.subcategoriaId()),
                contexto, reloj.instant());
        resultados.guardar(r);
        return r;
    }
}
