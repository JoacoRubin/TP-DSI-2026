package ar.utn.donatrack.front.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Hallazgo de la demo: "1 paradas", "1 entregas", "1 lote(s)". Las vistas usan @fmt.cantidad(n, singular, plural). */
class FormatoCantidadTest {

    private final Formato fmt = new Formato();

    @Test
    void usaElSingularSoloConUno() {
        assertThat(fmt.cantidad(1, "parada", "paradas")).isEqualTo("1 parada");
        assertThat(fmt.cantidad(3, "parada", "paradas")).isEqualTo("3 paradas");
        assertThat(fmt.cantidad(0, "entrega", "entregas")).isEqualTo("0 entregas");
    }

    @Test
    void aceptaLosNumerosComoLleganDelJson() {
        assertThat(fmt.cantidad(Long.valueOf(1), "lote", "lotes")).isEqualTo("1 lote");
        assertThat(fmt.cantidad(null, "lote", "lotes")).isEqualTo("0 lotes");
    }
}
