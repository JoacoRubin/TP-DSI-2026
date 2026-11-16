package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.EnMemoria;
import ar.utn.donatrack.donaciones.asignacion.domain.CompatibilidadSemantica;
import ar.utn.donatrack.donaciones.asignacion.domain.MotorMatchmaking;
import ar.utn.donatrack.donaciones.asignacion.domain.PrioridadSubAtendidos;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfirmarAsignacionTest {

    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final EnMemoria.Donaciones donaciones = new EnMemoria.Donaciones();
    private final EnMemoria.Necesidades necesidades = new EnMemoria.Necesidades();
    private final EnMemoria.Entidades entidades = new EnMemoria.Entidades();
    private final EnMemoria.Donantes donantes = new EnMemoria.Donantes();
    private final EnMemoria.Resultados resultados = new EnMemoria.Resultados();
    private final EnMemoria.LogisticaFalsa logistica = new EnMemoria.LogisticaFalsa();
    private final EnMemoria.NotificadorFalso notificador = new EnMemoria.NotificadorFalso();

    private ConfirmarAsignacion confirmar;
    private EjecutarMatchmaking ejecutar;
    private Donacion donacion;
    private Necesidad necesidad;
    private EntidadBeneficiaria escuela;
    private final Solicitante admin = new Solicitante("admin@donatrack.org", "ADMIN", null);

    @BeforeEach
    void preparar() {
        PersonaHumana ana = PersonaHumana.registrar(UUID.randomUUID(), "Ana", "Pérez", 30,
                new Documento(TipoDocumento.DNI, "12345678"), Genero.FEMENINO, direccion(),
                new MediosContacto("ana@mail.com", null, null, MedioNotificacion.EMAIL), AHORA);
        donantes.guardar(ana);
        escuela = entidad("Escuela Rural 10");
        entidades.guardar(escuela);
        Donacion base = donacion(SILLAS, "sillas", 10);
        donacion = Donacion.registrar(base.id(), base.cargaId(), ana.id(), "Sillas", SILLAS.id(), SILLAS.nombre(),
                "Mobiliario", "unidades", null, null, base.bienes(), 5, 0.1, "admin", AHORA);
        donaciones.guardar(donacion);
        necesidad = extraordinaria(escuela, SILLAS, "sillas", 30);
        necesidades.guardar(necesidad);

        EvaluadorDonaciones evaluador = new EvaluadorDonaciones(new MotorMatchmaking(List.of(
                new CompatibilidadSemantica(resultados::pesos), new PrioridadSubAtendidos(0.1))),
                necesidades, donaciones, resultados, reloj);
        ejecutar = new EjecutarMatchmaking(evaluador, donaciones);
        confirmar = new ConfirmarAsignacion(donaciones, necesidades, entidades, donantes, resultados, logistica,
                notificador, new Enlaces("http://localhost:8080"), reloj);
    }

    @Test
    void exigeHaberEjecutadoLosAlgoritmosAntes() {
        assertThatThrownBy(() -> confirmar.ejecutar(donacion.id(), necesidad.id(), admin))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("algoritmos");
    }

    @Test
    void confirmarAsignaComprometeSolicitaEntregaYNotificaAAmbos() {
        assertThat(ejecutar.ejecutar()).isEqualTo(1);

        confirmar.ejecutar(donacion.id(), necesidad.id(), admin);

        assertThat(donacion.estado()).isEqualTo(EstadoDonacion.ASIGNADA);
        assertThat(necesidad.cantidadComprometida()).isEqualTo(10);
        assertThat(logistica.solicitudes).singleElement().satisfies(s -> {
            assertThat(s.entidadNombre()).isEqualTo("Escuela Rural 10");
            assertThat(s.latitud()).isEqualTo(escuela.direccion().coordenadas().latitud());
        });
        assertThat(notificador.deTipo("DONACION_ASIGNADA")).extracting(e -> e.destinatario().email())
                .containsExactlyInAnyOrder("ana@mail.com", escuela.emailPrincipal());
        assertThat(resultados.deDonacion(donacion.id())).isEmpty();
    }

    @Test
    void noPermiteElegirUnaNecesidadQueNoFuePropuesta() {
        ejecutar.ejecutar();
        Necesidad otra = extraordinaria(escuela, MESAS, "mesas", 3);
        necesidades.guardar(otra);
        assertThatThrownBy(() -> confirmar.ejecutar(donacion.id(), otra.id(), admin))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void siLogisticaNoRespondeSePropagaElErrorParaRevertirLaTransaccion() {
        ejecutar.ejecutar();
        logistica.caida = true;
        assertThatThrownBy(() -> confirmar.ejecutar(donacion.id(), necesidad.id(), admin))
                .isInstanceOf(IllegalStateException.class);
        assertThat(notificador.enviadas).isEmpty();
    }
}
