package ar.utn.donatrack.contratos;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;

import java.util.Map;

/**
 * Registro de tipos lógicos de mensaje ↔ clase. Viaja en una propiedad del mensaje en lugar
 * del nombre completo de la clase: así productor y consumidor pueden evolucionar sus paquetes
 * sin romper el contrato.
 */
public final class TiposMensaje {

    public static final String PROPIEDAD_TIPO = "_tipo";

    public static final Map<String, Class<?>> TODOS = Map.of(
            "notificacion.solicitada", SolicitudNotificacion.class,
            "donacion.registrada", EventosDonaciones.DonacionRegistrada.class,
            "donacion.entregada", EventosDonaciones.DonacionEntregada.class,
            "entrega.planificada", EventosLogistica.EntregaPlanificada.class,
            "ruta.iniciada", EventosLogistica.RutaIniciada.class,
            "entrega.realizada", EventosLogistica.EntregaRealizada.class,
            "entrega.no-satisfactoria", EventosLogistica.EntregaNoSatisfactoria.class,
            "entrega.reprogramada", EventosLogistica.EntregaReprogramada.class);

    private TiposMensaje() {
    }
}
