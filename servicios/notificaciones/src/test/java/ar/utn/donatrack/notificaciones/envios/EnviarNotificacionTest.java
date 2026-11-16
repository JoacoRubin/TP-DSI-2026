package ar.utn.donatrack.notificaciones.envios;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.contratos.notificaciones.SolicitudNotificacion;
import ar.utn.donatrack.notificaciones.envios.application.EnviarNotificacion;
import ar.utn.donatrack.notificaciones.envios.application.ReintentarFallidas;
import ar.utn.donatrack.notificaciones.envios.domain.CanalNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import ar.utn.donatrack.notificaciones.envios.domain.NotificacionRepositorio;
import ar.utn.donatrack.notificaciones.envios.domain.SelectorCanal;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnviarNotificacionTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
    private final Repo repo = new Repo();

    static class CanalFalso implements CanalNotificacion {
        final MedioNotificacion medio;
        final List<String> enviados = new ArrayList<>();
        boolean fallar;

        CanalFalso(MedioNotificacion medio) { this.medio = medio; }
        public MedioNotificacion medio() { return medio; }
        public void enviar(Destinatario d, String asunto, String mensaje) {
            if (fallar) throw new IllegalStateException("proveedor caído");
            enviados.add(d.email() + ":" + asunto);
        }
    }

    private final CanalFalso email = new CanalFalso(MedioNotificacion.EMAIL);
    private final CanalFalso whatsapp = new CanalFalso(MedioNotificacion.WHATSAPP);
    private final CanalFalso sms = new CanalFalso(MedioNotificacion.SMS);

    private EnviarNotificacion caso() {
        return new EnviarNotificacion(repo, new SelectorCanal(List.of(email, whatsapp, sms)), reloj);
    }

    private SolicitudNotificacion solicitud(Destinatario d) {
        return new SolicitudNotificacion(UUID.randomUUID(), d, "Asunto", "Mensaje", "PRUEBA", "test", null, reloj.instant());
    }

    @Test
    void usaElMedioPreferidoDelDestinatario() {
        caso().ejecutar(solicitud(new Destinatario("Ana", "ana@mail.com", null, "+5491155555555", MedioNotificacion.WHATSAPP)));
        assertThat(whatsapp.enviados).hasSize(1);
        assertThat(email.enviados).isEmpty();
    }

    @Test
    void siElMedioPreferidoNoTieneDatosCaeAEmail() {
        caso().ejecutar(solicitud(new Destinatario("Ana", "ana@mail.com", null, null, MedioNotificacion.SMS)));
        assertThat(email.enviados).hasSize(1);
        assertThat(sms.enviados).isEmpty();
    }

    @Test
    void unEnvioExitosoQuedaComoEnviada() {
        SolicitudNotificacion s = solicitud(Destinatario.porEmail("Ana", "ana@mail.com"));
        caso().ejecutar(s);
        assertThat(repo.buscar(s.id())).get().extracting(Notificacion::estado).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void siElProveedorFallaQuedaFallidaYLuegoSeReintenta() {
        email.fallar = true;
        SolicitudNotificacion s = solicitud(Destinatario.porEmail("Ana", "ana@mail.com"));
        caso().ejecutar(s);
        assertThat(repo.buscar(s.id())).get().extracting(Notificacion::estado).isEqualTo(EstadoNotificacion.FALLIDA);

        email.fallar = false;
        new ReintentarFallidas(repo, new SelectorCanal(List.of(email, whatsapp, sms)), reloj).ejecutar();
        assertThat(repo.buscar(s.id())).get().extracting(Notificacion::estado).isEqualTo(EstadoNotificacion.ENVIADA);
    }

    @Test
    void esIdempotenteAnteMensajesDuplicados() {
        SolicitudNotificacion s = solicitud(Destinatario.porEmail("Ana", "ana@mail.com"));
        caso().ejecutar(s);
        caso().ejecutar(s);
        assertThat(email.enviados).hasSize(1);
    }

    @Test
    void despuesDelMaximoDeIntentosNoSeReintentaMas() {
        email.fallar = true;
        SolicitudNotificacion s = solicitud(Destinatario.porEmail("Ana", "ana@mail.com"));
        caso().ejecutar(s);
        ReintentarFallidas reintentar = new ReintentarFallidas(repo, new SelectorCanal(List.of(email, whatsapp, sms)), reloj);
        for (int i = 0; i < 10; i++) {
            reintentar.ejecutar();
        }
        assertThat(repo.buscar(s.id())).get().extracting(Notificacion::intentos).isEqualTo(Notificacion.MAX_INTENTOS);
    }

    static class Repo implements NotificacionRepositorio {
        final Map<UUID, Notificacion> datos = new LinkedHashMap<>();
        public Optional<Notificacion> buscar(UUID id) { return Optional.ofNullable(datos.get(id)); }
        public void guardar(Notificacion n) { datos.put(n.id(), n); }
        public List<Notificacion> reintentables(int max) {
            return datos.values().stream().filter(n -> n.esReintentable()).limit(max).toList();
        }
        public List<Notificacion> deDestinatario(String email, int limite) {
            return datos.values().stream().filter(n -> n.destinatario().email().equals(email)).toList();
        }
    }
}
