package ar.utn.donatrack.notificaciones.envios.infrastructure.canales;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.CanalNotificacion;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * SMS y WhatsApp vía la API REST de Twilio (sin SDK: un POST form-urlencoded con Basic Auth).
 * WhatsApp usa el mismo endpoint con el prefijo "whatsapp:" en los números.
 */
public class CanalTwilio implements CanalNotificacion {

    /** Twilio va por Internet: más margen que entre servicios internos, pero nunca infinito. */
    static final Duration CONEXION = Duration.ofSeconds(5);
    static final Duration LECTURA = Duration.ofSeconds(10);

    private final MedioNotificacion medio;
    private final RestClient cliente;
    private final String accountSid;
    private final String numeroOrigen;

    public CanalTwilio(MedioNotificacion medio, String accountSid, String authToken, String numeroOrigen) {
        this(medio, accountSid, authToken, numeroOrigen, "https://api.twilio.com/2010-04-01");
    }

    /** Para apuntar a un Twilio simulado en los tests. */
    CanalTwilio(MedioNotificacion medio, String accountSid, String authToken, String numeroOrigen, String baseUrl) {
        if (medio == MedioNotificacion.EMAIL) {
            throw new IllegalArgumentException("Twilio sólo maneja SMS y WhatsApp");
        }
        this.medio = medio;
        this.accountSid = accountSid;
        this.numeroOrigen = numeroOrigen;
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONEXION).build());
        fabrica.setReadTimeout(LECTURA);
        this.cliente = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .defaultHeaders(h -> h.setBasicAuth(accountSid, authToken))
                .build();
    }

    @Override
    public MedioNotificacion medio() {
        return medio;
    }

    @Override
    public void enviar(Destinatario d, String asunto, String mensaje) {
        String prefijo = medio == MedioNotificacion.WHATSAPP ? "whatsapp:" : "";
        String destino = medio == MedioNotificacion.WHATSAPP ? d.whatsapp() : d.telefono();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("From", prefijo + numeroOrigen);
        form.add("To", prefijo + destino.replaceAll("[^+\\d]", ""));
        form.add("Body", (asunto == null ? "" : asunto + "\n") + mensaje);
        cliente.post()
                .uri("/Accounts/{sid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
