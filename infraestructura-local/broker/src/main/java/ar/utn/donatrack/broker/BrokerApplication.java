package ar.utn.donatrack.broker;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Broker Artemis embebido que acepta conexiones TCP de los demás servicios.
 * Política: 5 reintentos con backoff y luego Dead Letter Queue (DLQ), para que un mensaje
 * "venenoso" no bloquee la cola.
 */
@SpringBootApplication
public class BrokerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrokerApplication.class, args);
    }

    @Bean
    ArtemisConfigurationCustomizer aceptorTcp(@Value("${donatrack.broker.aceptor}") String aceptor) {
        return configuracion -> {
            try {
                configuracion.addAcceptorConfiguration("tcp", aceptor);
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo configurar el aceptor TCP " + aceptor, e);
            }
            configuracion.addAddressSetting("#", new AddressSettings()
                    .setDeadLetterAddress(SimpleString.of("DLQ"))
                    .setMaxDeliveryAttempts(5)
                    .setRedeliveryDelay(2000)
                    .setRedeliveryMultiplier(2.0));
        };
    }
}
