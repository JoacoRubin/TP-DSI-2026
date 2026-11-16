package ar.utn.donatrack.autenticacion.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * @param clientesServicio clientId → secreto de cada servicio interno habilitado
 * @param directorioClaves dónde persistir el par RSA
 */
@ConfigurationProperties(prefix = "donatrack.autenticacion")
public record PropiedadesAutenticacion(
        Map<String, String> clientesServicio,
        String directorioClaves,
        Duration vigenciaTokenUsuario,
        Duration vigenciaTokenServicio) {
}
