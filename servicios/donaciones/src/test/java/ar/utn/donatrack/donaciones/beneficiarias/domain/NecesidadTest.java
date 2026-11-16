package ar.utn.donatrack.donaciones.beneficiarias.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NecesidadTest {

    private final EntidadBeneficiaria escuela = entidad("Escuela Rural 10");

    @Test
    void extraordinaria_seSatisfaceConDonacionesParcialesHastaAlcanzarLaCantidad() {
        NecesidadExtraordinaria sillas = extraordinaria(escuela, SILLAS, "30 sillas por inundación", 30);

        sillas.registrarRecepcion(2, HOY);
        sillas.registrarRecepcion(20, HOY);
        assertThat(sillas.estaSatisfecha(HOY)).isFalse();
        assertThat(sillas.cantidadPendiente(HOY)).isEqualTo(8);

        sillas.registrarRecepcion(10, HOY);
        assertThat(sillas.estaSatisfecha(HOY)).isTrue();
        assertThat(sillas.admiteDonaciones(HOY)).isFalse();
    }

    @Test
    void lasCantidadesComprometidasReducenLoPendienteHastaQueSeRecibenOLiberan() {
        NecesidadExtraordinaria sillas = extraordinaria(escuela, SILLAS, "sillas", 30);
        sillas.comprometer(20);
        assertThat(sillas.cantidadPendiente(HOY)).isEqualTo(10);

        sillas.registrarRecepcion(20, HOY);
        assertThat(sillas.cantidadComprometida()).isZero();
        assertThat(sillas.cantidadPendiente(HOY)).isEqualTo(10);

        sillas.comprometer(5);
        sillas.liberar(5);
        assertThat(sillas.cantidadPendiente(HOY)).isEqualTo(10);
    }

    @Test
    void recurrente_seRenuevaEnCadaPeriodo() {
        NecesidadRecurrente fideos = recurrente(entidad("Comedor Escobar Sonrisas"), FIDEOS, "100 paquetes por semana", 100);

        fideos.registrarRecepcion(100, HOY);
        assertThat(fideos.estaSatisfecha(HOY)).isTrue();

        assertThat(fideos.estaSatisfecha(HOY.plusWeeks(1))).isFalse();
        assertThat(fideos.cantidadPendiente(HOY.plusWeeks(1))).isEqualTo(100);
    }

    @Test
    void laCantidadRequeridaDebeSerPositiva() {
        assertThatThrownBy(() -> extraordinaria(escuela, SILLAS, "sillas", 0)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void unaNecesidadDadaDeBajaNoAdmiteDonaciones() {
        NecesidadExtraordinaria sillas = extraordinaria(escuela, SILLAS, "sillas", 30);
        sillas.darDeBaja();
        assertThat(sillas.admiteDonaciones(HOY)).isFalse();
    }

    @Test
    void periodicidadMensualAgrupaPorMesCalendario() {
        assertThat(Periodicidad.MENSUAL.mismoPeriodo(HOY, HOY.withDayOfMonth(1))).isTrue();
        assertThat(Periodicidad.MENSUAL.mismoPeriodo(HOY, HOY.plusMonths(1))).isFalse();
        assertThat(Periodicidad.SEMANAL.mismoPeriodo(HOY, HOY.plusDays(1))).isTrue(); // lunes y martes
    }
}
