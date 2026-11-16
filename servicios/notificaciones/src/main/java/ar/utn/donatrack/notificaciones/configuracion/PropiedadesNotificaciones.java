package ar.utn.donatrack.notificaciones.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param emailModo  "simulado" o "smtp"
 * @param twilioModo "simulado" o "real"
 */
@ConfigurationProperties(prefix = "donatrack.notificaciones")
public record PropiedadesNotificaciones(
        String emailModo,
        String remitente,
        String twilioModo,
        String twilioAccountSid,
        String twilioAuthToken,
        String twilioNumeroSms,
        String twilioNumeroWhatsapp) {
}
