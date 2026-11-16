package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static ar.utn.donatrack.donaciones.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class MotorMatchmakingTest {

    private ContextoMatchmaking contexto(Map<UUID, Integer> recibidas) {
        return new ContextoMatchmaking() {
            public LocalDate hoy() { return HOY; }
            public int donacionesRecibidasEnTrimestre(UUID entidadId) { return recibidas.getOrDefault(entidadId, 0); }
        };
    }

    private final MotorMatchmaking motor = new MotorMatchmaking(List.of(
            new CompatibilidadSemantica(() -> PesosSemanticos.PREDETERMINADOS),
            new PrioridadSubAtendidos(0.1)));

    @Test
    void soloEvaluaNecesidadesDeLaMismaSubcategoria() {
        Donacion sillas = donacion(SILLAS, "sillas", 5);
        EntidadBeneficiaria e = entidad("Escuela");
        Necesidad deSillas = extraordinaria(e, SILLAS, "sillas", 5);
        Necesidad deMesas = extraordinaria(e, MESAS, "sillas", 5);

        ResultadoMatchmaking r = motor.evaluar(sillas, List.of(deSillas, deMesas), contexto(Map.of()), AHORA);

        assertThat(r.rankings()).allSatisfy(rk -> assertThat(rk.puntajes())
                .extracting(Puntaje::necesidadId).containsExactly(deSillas.id()));
    }

    @Test
    void proponeComoMaximoDiezNecesidadesOrdenadasPorPuntaje() {
        Donacion sillas = donacion(SILLAS, "sillas", 5);
        List<Necesidad> candidatas = new ArrayList<>();
        IntStream.range(0, 15).forEach(i -> candidatas.add(extraordinaria(entidad("E" + i), SILLAS, "sillas", 5 + i)));

        ResultadoMatchmaking r = motor.evaluar(sillas, candidatas, contexto(Map.of()), AHORA);

        assertThat(r.rankings()).allSatisfy(rk -> {
            assertThat(rk.puntajes()).hasSize(MotorMatchmaking.TOP);
            assertThat(rk.puntajes()).extracting(Puntaje::valor).isSortedAccordingTo((a, b) -> Double.compare(b, a));
            assertThat(rk.puntajes()).allSatisfy(p -> assertThat(p.valor()).isBetween(0.0, 1.0));
        });
    }

    @Test
    void lasCoincidenciasSonLasNecesidadesPresentesEnElTopDeTodosLosAlgoritmos() {
        Donacion sillas = donacion(SILLAS, "sillas", 30);
        EntidadBeneficiaria mimada = entidad("Mimada");
        EntidadBeneficiaria olvidada = entidad("Olvidada");
        Necesidad n1 = extraordinaria(mimada, SILLAS, "sillas", 30);
        Necesidad n2 = extraordinaria(olvidada, SILLAS, "sillas", 30);

        ResultadoMatchmaking r = motor.evaluar(sillas, List.of(n1, n2), contexto(Map.of(mimada.id(), 50)), AHORA);

        assertThat(r.hayCoincidencias()).isTrue();
        assertThat(r.coincidencias()).containsExactly(n2.id(), n1.id());
    }

    @Test
    void ignoraNecesidadesYaSatisfechas() {
        Donacion sillas = donacion(SILLAS, "sillas", 5);
        NecesidadExtraordinaria satisfecha = extraordinaria(entidad("E"), SILLAS, "sillas", 5);
        satisfecha.registrarRecepcion(5, HOY);

        ResultadoMatchmaking r = motor.evaluar(sillas, List.of(satisfecha), contexto(Map.of()), AHORA);

        assertThat(r.rankings()).allSatisfy(rk -> assertThat(rk.puntajes()).isEmpty());
        assertThat(r.hayCoincidencias()).isFalse();
    }
}
