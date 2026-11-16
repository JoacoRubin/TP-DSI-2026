package ar.utn.donatrack.contratos.notificaciones;

import java.time.Instant;
import java.util.UUID;

/**
 * Mensaje que viaja por la cola {@link ar.utn.donatrack.contratos.Colas#NOTIFICACIONES}.
 *
 * @param id     identificador idempotente (si llega dos veces, se procesa una sola)
 * @param tipo   evento de negocio que la originó (DONACION_ASIGNADA, MISION_CUMPLIDA, ...)
 * @param origen servicio emisor
 * @param medio  medio forzado; si es null se usa el preferido del destinatario
 */
public record SolicitudNotificacion(
        UUID id,
        Destinatario destinatario,
        String asunto,
        String mensaje,
        String tipo,
        String origen,
        MedioNotificacion medio,
        Instant solicitadaEn) {

    public static SolicitudNotificacion de(Destinatario destinatario, String asunto, String mensaje,
                                           String tipo, String origen) {
        return new SolicitudNotificacion(UUID.randomUUID(), destinatario, asunto, mensaje, tipo, origen,
                null, Instant.now());
    }
}
