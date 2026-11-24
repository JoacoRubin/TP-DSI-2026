package ar.utn.donatrack.incentivos.misiones.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgo de la demo: el post salía con el emoji y "#DonaTrack #UTNSolidaria" duplicados, porque los
 * agregaban Incentivos Y el flujo de n8n. Incentivos cuenta el hecho; el formato para redes (emoji,
 * @mención, hashtags) es del flujo de difusión.
 */
class TextoDifusionTest {

    @Test
    void cuentaElHechoSinFormatoDeRedSocial() {
        String texto = AplicadorProgreso.textoDifusion("Lucía Gómez", "Primer paso");

        assertThat(texto).isEqualTo("Lucía Gómez obtuvo la insignia \"Primer paso\" en DonaTrack. ¡Gracias por ayudar a quienes más lo necesitan!");
        assertThat(texto).doesNotContain("#").doesNotContain("@");
    }
}
