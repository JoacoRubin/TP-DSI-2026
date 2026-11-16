package ar.utn.donatrack.notificaciones.envios.infrastructure.canales;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.CanalNotificacion;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Integración real por SMTP (Gmail, Mailtrap, SES...). Se activa con modo email = smtp. */
public class CanalEmailSmtp implements CanalNotificacion {

    private final JavaMailSender mailSender;
    private final String remitente;

    public CanalEmailSmtp(JavaMailSender mailSender, String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public MedioNotificacion medio() {
        return MedioNotificacion.EMAIL;
    }

    @Override
    public void enviar(Destinatario d, String asunto, String mensaje) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(remitente);
        mail.setTo(d.email());
        mail.setSubject(asunto == null ? "DonaTrack" : asunto);
        mail.setText(mensaje);
        mailSender.send(mail);
    }
}
