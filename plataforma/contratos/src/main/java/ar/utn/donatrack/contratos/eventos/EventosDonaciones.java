package ar.utn.donatrack.contratos.eventos;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;

import java.time.Instant;
import java.util.UUID;

/** Eventos publicados por el Servicio de Donaciones y consumidos por Incentivos. */
public final class EventosDonaciones {

    private EventosDonaciones() {
    }

    /** Snapshot del donante: Incentivos lo desnormaliza para no consultar a Donaciones. */
    public record DonanteResumen(UUID id, String nombre, String email, String telefono, String whatsapp,
                                 MedioNotificacion medioPreferido, boolean personaHumana) {
    }

    public record DonacionRegistrada(UUID donacionId, UUID cargaId, DonanteResumen donante,
                                     String categoria, String subcategoria, int cantidad,
                                     Instant registradaEn) {
    }

    public record DonacionEntregada(UUID donacionId, UUID donanteId, UUID entidadId, Instant entregadaEn) {
    }
}
