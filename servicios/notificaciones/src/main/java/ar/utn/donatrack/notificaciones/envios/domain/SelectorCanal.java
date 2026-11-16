package ar.utn.donatrack.notificaciones.envios.domain;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Elige el canal: el medio pedido explícitamente, si no el preferido del destinatario, y si
 * ese medio no tiene datos de contacto (ej. prefiere SMS pero no cargó teléfono) cae a EMAIL,
 * que es obligatorio para todas las personas.
 */
public class SelectorCanal {

    private final Map<MedioNotificacion, CanalNotificacion> canales = new EnumMap<>(MedioNotificacion.class);

    public SelectorCanal(List<CanalNotificacion> disponibles) {
        disponibles.forEach(c -> canales.put(c.medio(), c));
        if (!canales.containsKey(MedioNotificacion.EMAIL)) {
            throw new IllegalStateException("El canal EMAIL es obligatorio");
        }
    }

    public CanalNotificacion elegir(Destinatario destinatario, MedioNotificacion solicitado) {
        MedioNotificacion medio = solicitado != null ? solicitado : destinatario.medioPreferido();
        if (!tieneDatos(destinatario, medio) || !canales.containsKey(medio)) {
            medio = MedioNotificacion.EMAIL;
        }
        return canales.get(medio);
    }

    private static boolean tieneDatos(Destinatario d, MedioNotificacion medio) {
        return switch (medio) {
            case EMAIL -> true;
            case SMS -> d.telefono() != null && !d.telefono().isBlank();
            case WHATSAPP -> d.whatsapp() != null && !d.whatsapp().isBlank();
        };
    }
}
