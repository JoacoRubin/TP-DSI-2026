package ar.utn.donatrack.contratos.eventos;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;

import java.time.Instant;
import java.util.UUID;

/** Eventos publicados por el Servicio de Donaciones y consumidos por Incentivos. */
public final class EventosDonaciones {

    private EventosDonaciones() {
    }

    /**
     * Snapshot del donante: Incentivos lo desnormaliza para no consultar a Donaciones.
     * {@code aceptaDifusionLogros}: consentimiento dado en el alta (Ley 25.326) para difundir sus logros en
     * redes. Sólo define el perfil NUEVO en Incentivos; después decide el donante desde su panel. Un mensaje
     * anterior a este campo llega sin él y se lee como {@code false}: sin consentimiento, no se difunde.
     */
    public record DonanteResumen(UUID id, String nombre, String email, String telefono, String whatsapp,
                                 MedioNotificacion medioPreferido, boolean personaHumana, boolean aceptaDifusionLogros) {
    }

    public record DonacionRegistrada(UUID donacionId, UUID cargaId, DonanteResumen donante,
                                     String categoria, String subcategoria, int cantidad,
                                     Instant registradaEn) {
    }

    public record DonacionEntregada(UUID donacionId, UUID donanteId, UUID entidadId, Instant entregadaEn) {
    }
}
