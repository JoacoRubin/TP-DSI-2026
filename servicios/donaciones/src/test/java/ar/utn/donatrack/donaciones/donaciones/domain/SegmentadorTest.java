package ar.utn.donatrack.donaciones.donaciones.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegmentadorTest {

    private final Segmentador segmentador = new Segmentador(CATALOGO);
    private final UUID donante = UUID.randomUUID();

    private List<Donacion> segmentar(List<BienDeclarado> bienes) {
        return segmentador.segmentar(new CargaDonacion(donante, "Carga", bienes), "admin", HOY, AHORA);
    }

    @Test
    void ejemploArcosPlateados_sillasYMesaGeneranDosDonaciones() {
        List<Donacion> donaciones = segmentar(List.of(
                new BienDeclarado("Silla de oficina", SILLAS.id(), 6, "unidades", null, EstadoUso.USADO, null, null, null),
                new BienDeclarado("Mesa rectangular", MESAS.id(), 1, "unidades", null, EstadoUso.USADO, null, null, null)));

        assertThat(donaciones).hasSize(2);
        assertThat(donaciones).extracting(Donacion::subcategoriaNombre).containsExactlyInAnyOrder("Sillas", "Mesas");
        assertThat(donaciones).allSatisfy(d -> assertThat(d.estado()).isEqualTo(EstadoDonacion.EN_DEPOSITO));
        assertThat(donaciones).extracting(Donacion::cargaId).containsOnly(donaciones.get(0).cargaId());
    }

    @Test
    void ejemploPlantaDePastas_alimentosConVencimientoSeAgrupanPorSubcategoria() {
        LocalDate vto = LocalDate.of(2027, 1, 1);
        List<Donacion> donaciones = segmentar(List.of(
                new BienDeclarado("Fideos tirabuzón", FIDEOS.id(), 60, "paquetes", vto, null, null, null, null),
                new BienDeclarado("Fideos mostachol", FIDEOS.id(), 40, "paquetes", vto, null, null, null, null),
                new BienDeclarado("Tomate tetra-pack", TOMATE.id(), 50, "unidades", vto, null, null, null, null)));

        assertThat(donaciones).hasSize(2);
        Donacion fideos = donaciones.stream().filter(d -> d.subcategoriaId().equals(FIDEOS.id())).findFirst().orElseThrow();
        assertThat(fideos.cantidadTotal()).isEqualTo(100);
        assertThat(fideos.bienes()).hasSize(2);
    }

    @Test
    void perecederosConDistintoVencimientoGeneranDonacionesSeparadas() {
        List<Donacion> donaciones = segmentar(List.of(
                new BienDeclarado("Fideos", FIDEOS.id(), 10, "paquetes", LocalDate.of(2027, 1, 1), null, null, null, null),
                new BienDeclarado("Fideos", FIDEOS.id(), 10, "paquetes", LocalDate.of(2027, 3, 1), null, null, null, null)));
        assertThat(donaciones).hasSize(2);
    }

    @Test
    void bienesNuevosYUsadosDeLaMismaSubcategoriaSeSeparan() {
        List<Donacion> donaciones = segmentar(List.of(
                new BienDeclarado("Campera", CAMPERAS.id(), 3, "unidades", null, EstadoUso.NUEVO, null, null, null),
                new BienDeclarado("Campera", CAMPERAS.id(), 5, "unidades", null, EstadoUso.USADO, null, null, null)));
        assertThat(donaciones).extracting(Donacion::estadoUso).containsExactlyInAnyOrder(EstadoUso.NUEVO, EstadoUso.USADO);
    }

    @Test
    void unPerecederoSinFechaDeVencimientoEsRechazado() {
        assertThatThrownBy(() -> segmentar(List.of(
                new BienDeclarado("Fideos", FIDEOS.id(), 10, "paquetes", null, null, null, null, null))))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("vencimiento");
    }

    @Test
    void unPerecederoYaVencidoNoPuedeIngresar() {
        assertThatThrownBy(() -> segmentar(List.of(
                new BienDeclarado("Fideos", FIDEOS.id(), 10, "paquetes", HOY.minusDays(1), null, null, null, null))))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("vencid");
    }

    @Test
    void mobiliarioOVestimentaRequierenIndicarSiEsNuevoOUsado() {
        assertThatThrownBy(() -> segmentar(List.of(
                new BienDeclarado("Silla", SILLAS.id(), 1, "unidades", null, null, null, null, null))))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("nuevo o usado");
    }

    @Test
    void laCargaDebeTenerAlMenosUnBienYCantidadesPositivas() {
        assertThatThrownBy(() -> segmentar(List.of())).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> segmentar(List.of(
                new BienDeclarado("Silla", SILLAS.id(), 0, "unidades", null, EstadoUso.NUEVO, null, null, null))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void unaSubcategoriaInexistenteEsRechazada() {
        assertThatThrownBy(() -> segmentar(List.of(
                new BienDeclarado("X", UUID.randomUUID(), 1, "unidades", null, null, null, null, null))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
