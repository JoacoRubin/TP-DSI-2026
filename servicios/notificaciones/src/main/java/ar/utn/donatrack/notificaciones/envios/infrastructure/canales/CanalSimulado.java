package ar.utn.donatrack.notificaciones.envios.infrastructure.canales;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.CanalNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simula la llamada al proveedor (Entrega 1). Útil en desarrollo para no gastar créditos. */
public class CanalSimulado implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(CanalSimulado.class);
    private final MedioNotificacion medio;

    public CanalSimulado(MedioNotificacion medio) {
        this.medio = medio;
    }

    @Override
    public MedioNotificacion medio() {
        return medio;
    }

    @Override
    public void enviar(Destinatario d, String asunto, String mensaje) {
        String contacto = switch (medio) {
            case EMAIL -> d.email();
            case SMS -> d.telefono();
            case WHATSAPP -> d.whatsapp();
        };
        log.info("[{} SIMULADO] → {} <{}> | {} | {}", medio, d.nombre(), contacto, asunto, mensaje);
    }
}
