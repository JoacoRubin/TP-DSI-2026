package ar.utn.donatrack.donaciones.donaciones.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DonacionEstadosTest {

    private Donacion nueva() {
        return donacion(SILLAS, "Silla", 6);
    }

    @Test
    void alRegistrarseQuedaEnDepositoConHistorialInicial() {
        Donacion d = nueva();
        assertThat(d.estado()).isEqualTo(EstadoDonacion.EN_DEPOSITO);
        assertThat(d.historial()).hasSize(1);
        assertThat(d.historial().get(0).hasta()).isEqualTo(EstadoDonacion.EN_DEPOSITO);
    }

    @Test
    void recorridoFelizCompletoQuedaAuditado() {
        Donacion d = nueva();
        UUID necesidad = UUID.randomUUID();
        UUID entidad = UUID.randomUUID();
        d.asignarA(necesidad, entidad, "admin@donatrack.org", AHORA);
        d.marcarListaParaEntregar(AHORA);
        d.iniciarTraslado("AE123BC", AHORA);
        d.confirmarEntrega("AE123BC", AHORA);

        assertThat(d.estado()).isEqualTo(EstadoDonacion.ENTREGADA);
        assertThat(d.entidadId()).isEqualTo(entidad);
        assertThat(d.patenteEntrega()).isEqualTo("AE123BC");
        assertThat(d.historial()).extracting(CambioEstado::hasta).containsExactly(
                EstadoDonacion.EN_DEPOSITO, EstadoDonacion.ASIGNADA, EstadoDonacion.LISTA_PARA_ENTREGAR,
                EstadoDonacion.EN_TRASLADO, EstadoDonacion.ENTREGADA);
    }

    @Test
    void noSePuedeSaltearEstados() {
        Donacion d = nueva();
        assertThatThrownBy(() -> d.iniciarTraslado("AE123BC", AHORA))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("En depósito").hasMessageContaining("En traslado");
    }

    @Test
    void unaEntregaFallidaExigeJustificacionYPuedeReplanificarse() {
        Donacion d = nueva();
        d.asignarA(UUID.randomUUID(), UUID.randomUUID(), "admin", AHORA);
        d.marcarListaParaEntregar(AHORA);
        d.iniciarTraslado("AE123BC", AHORA);

        assertThatThrownBy(() -> d.registrarEntregaFallida(" ", AHORA)).isInstanceOf(ReglaDeNegocioException.class);
        d.registrarEntregaFallida("Tocamos timbre pero nadie respondió", AHORA);
        assertThat(d.estado()).isEqualTo(EstadoDonacion.ENTREGA_FALLIDA);
        assertThat(d.historial().get(d.historial().size() - 1).motivo()).contains("timbre");

        d.marcarListaParaEntregar(AHORA);
        assertThat(d.estado()).isEqualTo(EstadoDonacion.LISTA_PARA_ENTREGAR);
    }

    @Test
    void unaEntregaFallidaPuedeVolverAlDepositoYLiberaLaAsignacion() {
        Donacion d = nueva();
        d.asignarA(UUID.randomUUID(), UUID.randomUUID(), "admin", AHORA);
        d.marcarListaParaEntregar(AHORA);
        d.iniciarTraslado("AE123BC", AHORA);
        d.registrarEntregaFallida("Entidad cerrada", AHORA);

        d.devolverADeposito("admin", "Se reasignará", AHORA);

        assertThat(d.estado()).isEqualTo(EstadoDonacion.EN_DEPOSITO);
        assertThat(d.entidadId()).isNull();
        assertThat(d.necesidadId()).isNull();
    }

    @Test
    void sePuedeMarcarVencidaSoloMientrasEstaEnElDeposito() {
        Donacion enDeposito = nueva();
        enDeposito.marcarVencida("admin", "Fecha superada", AHORA);
        assertThat(enDeposito.estado()).isEqualTo(EstadoDonacion.VENCIDA);

        Donacion enViaje = nueva();
        enViaje.asignarA(UUID.randomUUID(), UUID.randomUUID(), "admin", AHORA);
        enViaje.marcarListaParaEntregar(AHORA);
        enViaje.iniciarTraslado("AE123BC", AHORA);
        assertThatThrownBy(() -> enViaje.marcarVencida("admin", "x", AHORA)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void unaDonacionEntregadaEsEstadoFinal() {
        Donacion d = nueva();
        d.asignarA(UUID.randomUUID(), UUID.randomUUID(), "admin", AHORA);
        d.marcarListaParaEntregar(AHORA);
        d.iniciarTraslado("AE123BC", AHORA);
        d.confirmarEntrega("AE123BC", AHORA);
        assertThat(EstadoDonacion.ENTREGADA.siguientes()).isEmpty();
        assertThatThrownBy(() -> d.marcarVencida("admin", "x", AHORA)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void soloSePuedeEliminarSiTodaviaEstaEnDeposito() {
        Donacion d = nueva();
        assertThat(d.puedeEliminarse()).isTrue();
        d.asignarA(UUID.randomUUID(), UUID.randomUUID(), "admin", AHORA);
        assertThat(d.puedeEliminarse()).isFalse();
    }
}
