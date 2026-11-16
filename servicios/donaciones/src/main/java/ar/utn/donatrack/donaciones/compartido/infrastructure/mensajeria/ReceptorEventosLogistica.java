package ar.utn.donatrack.donaciones.compartido.infrastructure.mensajeria;

import ar.utn.donatrack.contratos.Colas;
import ar.utn.donatrack.contratos.eventos.EventosLogistica;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarEntregaNoSatisfactoria;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarEntregaPlanificada;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarEntregaRealizada;
import ar.utn.donatrack.donaciones.donaciones.application.RegistrarInicioDeRuta;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.slf4j.Logger;
import org.springframework.jms.support.converter.MessageConverter;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador de entrada: traduce eventos de Logística a casos de uso de Donaciones. */
@Component
class ReceptorEventosLogistica {

    private static final Logger log = LoggerFactory.getLogger(ReceptorEventosLogistica.class);

    private final RegistrarEntregaPlanificada planificada;
    private final RegistrarInicioDeRuta inicioRuta;
    private final RegistrarEntregaRealizada realizada;
    private final RegistrarEntregaNoSatisfactoria noSatisfactoria;

    private final MessageConverter conversor;

    ReceptorEventosLogistica(RegistrarEntregaPlanificada planificada, RegistrarInicioDeRuta inicioRuta,
                             RegistrarEntregaRealizada realizada, RegistrarEntregaNoSatisfactoria noSatisfactoria,
                             MessageConverter conversor) {
        this.planificada = planificada;
        this.inicioRuta = inicioRuta;
        this.realizada = realizada;
        this.noSatisfactoria = noSatisfactoria;
        this.conversor = conversor;
    }

    /**
     * Una cola transporta varios tipos de evento: se recibe el mensaje crudo y se convierte
     * explícitamente (con un parámetro {@code Object}, Spring inyectaría el Message sin convertir).
     */
    @JmsListener(destination = Colas.DONACIONES_DESDE_LOGISTICA)
    @Transactional
    public void recibir(Message mensaje) throws JMSException {
        Object evento = conversor.fromMessage(mensaje);
        log.debug("Evento de logística recibido: {}", evento);
        switch (evento) {
            case EventosLogistica.EntregaPlanificada e -> planificada.ejecutar(e);
            case EventosLogistica.RutaIniciada e -> inicioRuta.ejecutar(e);
            case EventosLogistica.EntregaRealizada e -> realizada.ejecutar(e);
            case EventosLogistica.EntregaNoSatisfactoria e -> noSatisfactoria.ejecutar(e);
            case EventosLogistica.EntregaReprogramada e ->
                    log.info("Entrega {} reprogramada: se actualizará al planificarse nuevamente", e.entregaId());
            default -> log.warn("Evento no reconocido: {}", evento.getClass().getName());
        }
    }
}
