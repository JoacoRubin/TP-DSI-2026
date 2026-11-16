package ar.utn.donatrack.soporte.mensajeria;

import ar.utn.donatrack.contratos.TiposMensaje;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

/**
 * Serializa los mensajes JMS como JSON. El tipo viaja como identificador lógico
 * ({@link TiposMensaje}) y no como nombre de clase, para no acoplar paquetes entre servicios.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration")
public class MensajeriaAutoConfiguration {

    @Bean
    public MessageConverter conversorMensajesJson(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter conversor = new MappingJackson2MessageConverter();
        conversor.setObjectMapper(objectMapper);
        conversor.setTargetType(MessageType.TEXT);
        conversor.setTypeIdPropertyName(TiposMensaje.PROPIEDAD_TIPO);
        conversor.setTypeIdMappings(TiposMensaje.TODOS);
        return conversor;
    }

    @Bean
    public PublicadorMensajes publicadorMensajes(JmsTemplate jmsTemplate) {
        return new PublicadorMensajes(jmsTemplate);
    }
}
