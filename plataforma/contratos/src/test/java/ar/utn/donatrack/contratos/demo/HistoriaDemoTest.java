package ar.utn.donatrack.contratos.demo;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgo de la demo: Incentivos sembraba 4 donaciones de Ana con IDs al azar que Donaciones no tenía
 * (el panel decía 7 donaciones y "Mis donaciones" listaba 6). Ahora los tres servicios siembran la MISMA
 * historia; este test fija sus invariantes.
 */
class HistoriaDemoTest {

    private final ZoneId zona = ZoneId.of("America/Argentina/Buenos_Aires");
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-24T15:00:00Z"), zona);

    private List<DatosDemo.DonacionDemo> de(UUID donante) {
        return DatosDemo.HISTORIA.stream().filter(d -> d.donanteId().equals(donante)).toList();
    }

    @Test
    void cadaDonacionTieneUnIdPropioYFijo() {
        Set<UUID> ids = DatosDemo.HISTORIA.stream().map(DatosDemo.DonacionDemo::id).collect(Collectors.toSet());
        assertThat(ids).hasSize(DatosDemo.HISTORIA.size())
                .contains(DatosDemo.DONACION_HISTORICA_1, DatosDemo.DONACION_HISTORICA_2);
    }

    @Test
    void anaDonaCincoMesesSeguidosEnVariasCategorias() {
        Set<YearMonth> meses = de(DatosDemo.DONANTE_ANA).stream()
                .map(d -> YearMonth.from(d.registradaEn(reloj).atZone(zona))).collect(Collectors.toSet());
        assertThat(meses).contains(YearMonth.of(2026, 7), YearMonth.of(2026, 8), YearMonth.of(2026, 9),
                YearMonth.of(2026, 10), YearMonth.of(2026, 11));
        assertThat(de(DatosDemo.DONANTE_ANA).stream().map(DatosDemo.DonacionDemo::categoria).distinct().count())
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    void lasEntregadasTienenEntidadYSeEntreganDespuesDeRegistrarse() {
        DatosDemo.HISTORIA.stream().filter(DatosDemo.DonacionDemo::entregada).forEach(d -> {
            assertThat(d.entregadaA()).isNotNull();
            assertThat(d.entregadaEn(reloj)).isAfter(d.registradaEn(reloj)).isBefore(reloj.instant());
        });
        // Las del último mes alimentan la portada pública ("El último mes").
        assertThat(DatosDemo.HISTORIA.stream().filter(DatosDemo.DonacionDemo::entregada)
                .filter(d -> d.entregadaEn(reloj).isAfter(reloj.instant().minusSeconds(30 * 86_400L))).count())
                .isGreaterThanOrEqualTo(2);
    }

    @Test
    void quedanDonacionesEnDepositoParaProbarLaAsignacion() {
        assertThat(DatosDemo.HISTORIA.stream().filter(d -> !d.entregada()).count()).isGreaterThanOrEqualTo(3);
        assertThat(DatosDemo.HISTORIA.stream().filter(d -> !d.entregada()))
                .allSatisfy(d -> assertThat(d.registradaEn(reloj)).isEqualTo(reloj.instant()));
    }
}
