package ar.utn.donatrack.donaciones.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param urlFront           base pública del front (para los enlaces en notificaciones)
 * @param urlLogistica       base del Servicio de Logística
 * @param urlAutenticacion   base del Servicio de Autenticación
 * @param emailsAdministracion destinatarios de alertas operativas (entregas fallidas)
 * @param suavizadoSubAtendidos factor k del algoritmo de prioridad a sub-atendidos
 */
@ConfigurationProperties(prefix = "donatrack.donaciones")
public record PropiedadesDonaciones(String urlFront, String urlLogistica, String urlAutenticacion,
                                    List<String> emailsAdministracion, double suavizadoSubAtendidos) {
}
