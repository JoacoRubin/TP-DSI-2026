package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AlgoritmosMatchmakingTest {

    private final EntidadBeneficiaria escuela = entidad("Escuela");

    private ContextoMatchmaking contexto(Map<UUID, Integer> recibidas) {
        return new ContextoMatchmaking() {
            public LocalDate hoy() { return HOY; }
            public int donacionesRecibidasEnTrimestre(UUID entidadId) { return recibidas.getOrDefault(entidadId, 0); }
        };
    }

    @ParameterizedTest
    @CsvSource({"kitten,sitting,3", "silla,silla,0", "'',abc,3", "flaw,lawn,2"})
    void levenshteinCalculaLaDistanciaDeEdicion(String a, String b, int esperado) {
        assertThat(Levenshtein.distancia(a, b)).isEqualTo(esperado);
    }

    @Test
    void laSimilitudIgnoraMayusculasYAcentos() {
        assertThat(Levenshtein.similitud("Pantalón", "pantalon")).isEqualTo(1.0);
        assertThat(Levenshtein.similitud("silla", "xxxxx")).isEqualTo(0.0);
    }

    @Test
    void losPesosDebenSumarUno() {
        assertThatThrownBy(() -> new PesosSemanticos(0.5, 0.5, 0.5)).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(PesosSemanticos.PREDETERMINADOS.textual()).isEqualTo(0.4);
    }

    @Test
    void compatibilidadSemantica_aplicaElPolinomioPonderado() {
        Donacion d = donacion(SILLAS, "sillas", 15);
        Necesidad n = extraordinaria(escuela, SILLAS, "sillas", 30);
        // v1 = 1 (idénticos), r = 15/30 = 0.5 → v2 = 0.5, extraordinaria → v3 = 1
        double esperado = 0.4 * 1 + 0.4 * 0.5 + 0.2 * 1;

        double score = new CompatibilidadSemantica(() -> PesosSemanticos.PREDETERMINADOS).puntuar(d, n, contexto(Map.of()));

        assertThat(score).isCloseTo(esperado, within(1e-9));
    }

    @Test
    void compatibilidadSemantica_penalizaElDesperdicioCuandoSeDonaDeMas() {
        Donacion d = donacion(SILLAS, "sillas", 60);
        Necesidad n = extraordinaria(escuela, SILLAS, "sillas", 30);
        // r = 2 → v2 = 1/r = 0.5
        double score = new CompatibilidadSemantica(() -> new PesosSemanticos(0, 1, 0)).puntuar(d, n, contexto(Map.of()));
        assertThat(score).isCloseTo(0.5, within(1e-9));
    }

    @Test
    void compatibilidadSemantica_lasRecurrentesValenLaMitadEnElTipo() {
        Donacion d = donacion(FIDEOS, "fideos", 10);
        Necesidad n = recurrente(escuela, FIDEOS, "fideos", 10);
        double score = new CompatibilidadSemantica(() -> new PesosSemanticos(0, 0, 1)).puntuar(d, n, contexto(Map.of()));
        assertThat(score).isEqualTo(0.5);
    }

    @Test
    void prioridadSubAtendidos_decreceGradualmenteConLasDonacionesRecibidas() {
        Donacion d = donacion(SILLAS, "sillas", 1);
        Necesidad n = extraordinaria(escuela, SILLAS, "sillas", 30);
        PrioridadSubAtendidos algoritmo = new PrioridadSubAtendidos(0.1);

        assertThat(algoritmo.puntuar(d, n, contexto(Map.of()))).isEqualTo(1.0);
        assertThat(algoritmo.puntuar(d, n, contexto(Map.of(escuela.id(), 1)))).isCloseTo(1 / 1.1, within(1e-9));
        assertThat(algoritmo.puntuar(d, n, contexto(Map.of(escuela.id(), 10)))).isCloseTo(0.5, within(1e-9));
    }
}
