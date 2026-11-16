package ar.utn.donatrack.notificaciones.envios.domain;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;

/**
 * Strategy: cada medio de notificación es una implementación intercambiable.
 * Agregar Telegram mañana = agregar una clase, sin tocar el resto (Open/Closed).
 */
public interface CanalNotificacion {

    MedioNotificacion medio();

    /** @throws RuntimeException si el proveedor externo falla (se reintentará). */
    void enviar(Destinatario destinatario, String asunto, String mensaje);
}
