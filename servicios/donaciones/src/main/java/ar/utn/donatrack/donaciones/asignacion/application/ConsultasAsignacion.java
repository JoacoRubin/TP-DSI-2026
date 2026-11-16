package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.asignacion.domain.Puntaje;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.ResultadoMatchmakingRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ConsultasAsignacion {

    public record Pendiente(Donacion donacion, boolean evaluada, int coincidencias) {
    }

    /** Ranking enriquecido con los datos de necesidad y entidad para mostrar en pantalla. */
    public record Detalle(Donacion donacion, Optional<ResultadoMatchmaking> resultado, Map<UUID, Necesidad> necesidades,
                          Map<UUID, EntidadBeneficiaria> entidades) {
    }

    private final DonacionRepositorio donaciones;
    private final ResultadoMatchmakingRepositorio resultados;
    private final NecesidadRepositorio necesidades;
    private final EntidadRepositorio entidades;

    public ConsultasAsignacion(DonacionRepositorio donaciones, ResultadoMatchmakingRepositorio resultados,
                               NecesidadRepositorio necesidades, EntidadRepositorio entidades) {
        this.donaciones = donaciones;
        this.resultados = resultados;
        this.necesidades = necesidades;
        this.entidades = entidades;
    }

    public List<Pendiente> pendientes() {
        return donaciones.enEstado(EstadoDonacion.EN_DEPOSITO).stream()
                .map(d -> {
                    Optional<ResultadoMatchmaking> r = resultados.deDonacion(d.id());
                    return new Pendiente(d, r.isPresent(), r.map(x -> x.coincidencias().size()).orElse(0));
                })
                .toList();
    }

    public Detalle detalle(UUID donacionId) {
        Donacion d = donaciones.buscar(donacionId).orElseThrow(() -> new RecursoNoEncontradoException("Donación", donacionId));
        Optional<ResultadoMatchmaking> r = resultados.deDonacion(donacionId);
        Map<UUID, Necesidad> ns = new HashMap<>();
        r.ifPresent(res -> res.rankings().forEach(rk -> rk.puntajes().stream().map(Puntaje::necesidadId)
                .forEach(id -> necesidades.buscar(id).ifPresent(n -> ns.put(id, n)))));
        Map<UUID, EntidadBeneficiaria> es = entidades.buscarTodas(ns.values().stream().map(Necesidad::entidadId).toList());
        return new Detalle(d, r, ns, es);
    }
}
