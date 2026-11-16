package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Orquesta los algoritmos sin conocerlos (depende de la abstracción, no de las implementaciones).
 * Regla: una donación sólo se evalúa contra necesidades de EXACTAMENTE la misma subcategoría.
 */
public class MotorMatchmaking {

    public static final int TOP = 10;
    private final List<AlgoritmoMatchmaking> algoritmos;

    public MotorMatchmaking(List<AlgoritmoMatchmaking> algoritmos) {
        if (algoritmos.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un algoritmo");
        }
        this.algoritmos = List.copyOf(algoritmos);
    }

    public ResultadoMatchmaking evaluar(Donacion donacion, List<Necesidad> candidatas, ContextoMatchmaking ctx,
                                        Instant ahora) {
        List<Necesidad> compatibles = candidatas.stream()
                .filter(n -> n.subcategoriaId().equals(donacion.subcategoriaId()))
                .filter(n -> n.admiteDonaciones(ctx.hoy()))
                .toList();

        List<Ranking> rankings = algoritmos.stream()
                .map(a -> new Ranking(a.nombre(), compatibles.stream()
                        .map(n -> new Puntaje(n.id(), n.entidadId(), acotar(a.puntuar(donacion, n, ctx))))
                        .sorted(Comparator.comparingDouble(Puntaje::valor).reversed()
                                .thenComparing(p -> p.necesidadId().toString()))
                        .limit(TOP)
                        .toList()))
                .toList();

        List<UUID> coincidencias = rankings.get(0).puntajes().stream()
                .map(Puntaje::necesidadId)
                .filter(id -> rankings.stream().allMatch(r -> r.contiene(id)))
                .sorted(Comparator.comparingDouble((UUID id) -> promedio(rankings, id)).reversed())
                .toList();

        return new ResultadoMatchmaking(donacion.id(), ahora, rankings, coincidencias);
    }

    private static double promedio(List<Ranking> rankings, UUID necesidadId) {
        return rankings.stream().mapToDouble(r -> r.puntaje(necesidadId).map(Puntaje::valor).orElse(0.0)).average().orElse(0);
    }

    private static double acotar(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
