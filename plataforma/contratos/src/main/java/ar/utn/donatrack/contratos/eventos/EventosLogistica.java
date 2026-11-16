package ar.utn.donatrack.contratos.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Eventos publicados por Logística y consumidos por Donaciones. Son HECHOS (tiempo pasado):
 * Logística no le ordena nada a Donaciones, sólo informa. Así evitamos el acoplamiento
 * cíclico sincrónico Donaciones ⇄ Logística.
 */
public final class EventosLogistica {

    private EventosLogistica() {
    }

    public record EntregaPlanificada(UUID entregaId, UUID donacionId, UUID rutaId, String patente,
                                     Instant planificadaEn) {
    }

    public record RutaIniciada(UUID rutaId, String patente, List<UUID> donacionIds, String urlSeguimiento,
                               Instant iniciadaEn) {
    }

    public record EntregaRealizada(UUID entregaId, UUID donacionId, String patente, Instant entregadaEn) {
    }

    public record EntregaNoSatisfactoria(UUID entregaId, UUID donacionId, String motivo, Instant informadaEn) {
    }

    public record EntregaReprogramada(UUID entregaId, UUID donacionId, Instant reprogramadaEn) {
    }
}
