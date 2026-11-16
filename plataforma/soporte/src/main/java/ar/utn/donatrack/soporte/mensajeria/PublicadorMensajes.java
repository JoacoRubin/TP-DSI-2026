package ar.utn.donatrack.soporte.mensajeria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

/**
 * Fachada mínima sobre JmsTemplate. Los adaptadores de cada servicio la usan para implementar
 * sus puertos de salida (Notificador, PublicadorEventos, ...).
 */
public class PublicadorMensajes {

    private static final Logger log = LoggerFactory.getLogger(PublicadorMensajes.class);
    private final JmsTemplate jmsTemplate;

    public PublicadorMensajes(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void publicar(String cola, Object mensaje) {
        jmsTemplate.convertAndSend(cola, mensaje);
        log.debug("Mensaje {} publicado en {}", mensaje.getClass().getSimpleName(), cola);
    }
}
