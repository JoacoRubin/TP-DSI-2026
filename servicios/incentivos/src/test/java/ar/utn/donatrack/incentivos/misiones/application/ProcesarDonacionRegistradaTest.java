package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadDonacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.misiones.domain.CatalogoMisiones;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.MotorIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ley 25.326: difundir los logros de un donante en redes es un tratamiento de datos personales que
 * requiere consentimiento expreso. El perfil nace con lo que el donante eligió al registrarse.
 */
class ProcesarDonacionRegistradaTest {

    static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");

    final Map<UUID, PerfilDonante> perfiles = new HashMap<>();
    final List<ActividadDonacion> actividades = new ArrayList<>();
    final List<String> notificados = new ArrayList<>();
    final List<String> difundidas = new ArrayList<>();

    final ProcesarDonacionRegistrada caso = new ProcesarDonacionRegistrada(new Actividades(), new Perfiles(),
            new AplicadorProgreso(new MotorIncentivos(CatalogoMisiones.predeterminado()), new Actividades(), new Perfiles(),
                    new Insignias(), (d, asunto, mensaje, tipo) -> notificados.add(tipo),
                    (perfil, insignia, texto) -> difundidas.add(insignia.nombre()),
                    Clock.fixed(AHORA, ZoneOffset.UTC)),
            Clock.fixed(AHORA, ZoneOffset.UTC));

    private static EventosDonaciones.DonacionRegistrada primeraDonacion(UUID donante, boolean aceptaDifusion) {
        EventosDonaciones.DonanteResumen lucia = new EventosDonaciones.DonanteResumen(donante, "Lucía Gómez",
                "lucia@donatrack.org", null, null, MedioNotificacion.EMAIL, true, aceptaDifusion);
        return new EventosDonaciones.DonacionRegistrada(UUID.randomUUID(), UUID.randomUUID(), lucia,
                "Alimentos", "Fideos secos", 10, AHORA);
    }

    @Test
    void sinConsentimientoGanaLaInsigniaPeroNoSeDifunde() {
        UUID lucia = UUID.randomUUID();

        caso.ejecutar(primeraDonacion(lucia, false));

        assertThat(notificados).contains("MISION_CUMPLIDA"); // ella sí se entera de su logro
        assertThat(difundidas).isEmpty();
        assertThat(perfiles.get(lucia).insigniasVisibles()).isFalse();
    }

    @Test
    void conConsentimientoLaPrimeraInsigniaYaSeDifunde() {
        UUID lucia = UUID.randomUUID();

        caso.ejecutar(primeraDonacion(lucia, true));

        assertThat(difundidas).containsExactly("Primer paso");
        assertThat(perfiles.get(lucia).insigniasVisibles()).isTrue();
    }

    @Test
    void elConsentimientoSoloDefineElPerfilNuevoDespuesMandaElPanel() {
        UUID lucia = UUID.randomUUID();
        caso.ejecutar(primeraDonacion(lucia, true));
        perfiles.get(lucia).configurarVisibilidad(false); // la donante lo revocó desde su panel

        caso.ejecutar(primeraDonacion(lucia, true)); // eventos viejos no pisan su decisión

        assertThat(perfiles.get(lucia).insigniasVisibles()).isFalse();
    }

    // ------------------------------------------------------------------ fakes en memoria

    class Perfiles implements Repositorios.PerfilRepositorio {
        public Optional<PerfilDonante> buscar(UUID id) { return Optional.ofNullable(perfiles.get(id)); }
        public List<PerfilDonante> buscarTodos(List<UUID> ids) { return ids.stream().map(perfiles::get).toList(); }
        public void guardar(PerfilDonante p) { perfiles.put(p.donanteId(), p); }
    }

    class Actividades implements ActividadRepositorio {
        public Optional<ActividadDonacion> buscar(UUID id) {
            return actividades.stream().filter(a -> a.donacionId().equals(id)).findFirst();
        }
        public List<ActividadDonacion> deDonante(UUID id) {
            return actividades.stream().filter(a -> a.donanteId().equals(id)).toList();
        }
        public void guardar(ActividadDonacion a) { actividades.add(a); }
    }

    static class Insignias implements Repositorios.InsigniaRepositorio {
        public List<InsigniaObtenida> deDonante(UUID id) { return List.of(); }
        public List<InsigniaObtenida> obtenidasEntre(Instant desde, Instant hasta) { return List.of(); }
        public void guardarTodas(List<InsigniaObtenida> insignias) { }
    }
}
