package ar.utn.donatrack.donaciones.donaciones.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Hallazgo de la demo: "1 unidades de Mesas". La descripción viaja a vistas, avisos, comprobantes y Logística. */
class DescripcionCortaTest {

    private static Donacion de(int cantidad, String unidad) {
        return Donacion.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Prueba", UUID.randomUUID(),
                "Mesas", "Mobiliario", unidad, null, null, List.of(new Bien("Mesa", cantidad, null)), 1, 1, "admin",
                Instant.parse("2026-11-24T12:00:00Z"));
    }

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "1, unidades, 1 unidad de Mesas",
            "6, unidades, 6 unidades de Mesas",
            "1, paquetes, 1 paquete de Mesas",
            "1, cajas,    1 caja de Mesas",
            "1, litros,   1 litro de Mesas",
            "1, kg,       1 kg de Mesas",
            "2, kg,       2 kg de Mesas"
    })
    void laUnidadVaEnSingularCuandoEsUna(int cantidad, String unidad, String esperado) {
        assertThat(de(cantidad, unidad).descripcionCorta()).isEqualTo(esperado);
    }
}
