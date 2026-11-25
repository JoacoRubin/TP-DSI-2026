package ar.utn.donatrack.incentivos;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadDonacion;
import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;
import ar.utn.donatrack.incentivos.misiones.domain.CatalogoMisiones;
import ar.utn.donatrack.incentivos.misiones.domain.CategoriaDonante;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.Mision;
import ar.utn.donatrack.incentivos.misiones.domain.MotorIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.TipoMision;
import ar.utn.donatrack.incentivos.ranking.domain.CalculadorRanking;
import ar.utn.donatrack.incentivos.ranking.domain.RankingMensual;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DominioIncentivosTest {

    static final YearMonth NOV = YearMonth.of(2026, 11);
    static final Instant AHORA = Instant.parse("2026-11-16T12:00:00Z");
    final UUID donante = UUID.randomUUID();

    private ActividadDonacion donacion(YearMonth mes, String categoria, int cantidad) {
        return ActividadDonacion.registrada(UUID.randomUUID(), donante, UUID.randomUUID(), categoria, "sub", cantidad,
                mes.atDay(10).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private PerfilDonante perfil() {
        return PerfilDonante.nuevo(donante, "Ana", "ana@mail.com", null, null, MedioNotificacion.EMAIL, true, false, AHORA);
    }

    @Test
    void laRachaCuentaMesesConsecutivosHastaElMesActual() {
        HistorialDonante h = new HistorialDonante(List.of(
                donacion(NOV, "Alimentos", 1), donacion(NOV.minusMonths(1), "Alimentos", 1),
                donacion(NOV.minusMonths(2), "Alimentos", 1), donacion(NOV.minusMonths(5), "Alimentos", 1)), ZoneOffset.UTC);
        assertThat(h.rachaMeses(NOV)).isEqualTo(3);
    }

    @Test
    void laRachaToleraQueElMesEnCursoTodaviaNoTengaDonaciones() {
        HistorialDonante h = new HistorialDonante(List.of(donacion(NOV.minusMonths(1), "A", 1),
                donacion(NOV.minusMonths(2), "A", 1)), ZoneOffset.UTC);
        assertThat(h.rachaMeses(NOV)).isEqualTo(2);
    }

    @Test
    void laRachaSePierdeSiPasaUnMesCompletoSinDonar() {
        HistorialDonante h = new HistorialDonante(List.of(donacion(NOV.minusMonths(2), "A", 1),
                donacion(NOV.minusMonths(3), "A", 1)), ZoneOffset.UTC);
        assertThat(h.rachaMeses(NOV)).isZero();
    }

    @Test
    void habilDonadorMideLaMayorCargaYCompletitudLasCategoriasDistintas() {
        UUID carga = UUID.randomUUID();
        Instant t = AHORA;
        HistorialDonante h = new HistorialDonante(List.of(
                ActividadDonacion.registrada(UUID.randomUUID(), donante, carga, "Alimentos", "Fideos", 30, t),
                ActividadDonacion.registrada(UUID.randomUUID(), donante, carga, "Alimentos", "Arroz", 25, t),
                ActividadDonacion.registrada(UUID.randomUUID(), donante, UUID.randomUUID(), "Vestimenta", "Remeras", 5, t)),
                ZoneOffset.UTC);
        assertThat(TipoMision.HABIL_DONADOR.progreso(h, NOV)).isEqualTo(55);
        assertThat(TipoMision.COMPLETITUD.progreso(h, NOV)).isEqualTo(2);
    }

    @Test
    void lasMisionesSeCompletanEnOrdenYOtorganInsignia() {
        PerfilDonante p = perfil();
        CatalogoMisiones catalogo = CatalogoMisiones.predeterminado();
        HistorialDonante h = new HistorialDonante(List.of(donacion(NOV, "Alimentos", 3)), ZoneOffset.UTC);

        MotorIncentivos.Resultado r = new MotorIncentivos(catalogo).evaluar(p, h, NOV, AHORA);

        assertThat(r.insignias()).extracting(InsigniaObtenida::misionId).containsExactly("COL-1");
        assertThat(p.categoria()).isEqualTo(CategoriaDonante.COLABORADOR);
        assertThat(p.misionActual(catalogo)).get().extracting(Mision::id).isEqualTo("COL-2");
    }

    @Test
    void alCompletarTodasLasMisionesDeLaCategoriaAsciende() {
        PerfilDonante p = perfil();
        CatalogoMisiones catalogo = CatalogoMisiones.predeterminado();
        List<ActividadDonacion> acts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ActividadDonacion a = donacion(NOV.minusMonths(i), "Alimentos", 2);
            a.marcarEntregada(UUID.randomUUID(), AHORA);
            acts.add(a);
        }

        MotorIncentivos.Resultado r = new MotorIncentivos(catalogo).evaluar(p, new HistorialDonante(acts, ZoneOffset.UTC), NOV, AHORA);

        assertThat(r.insignias()).extracting(InsigniaObtenida::misionId).containsExactly("COL-1", "COL-2", "COL-3");
        assertThat(r.ascensos()).containsExactly(CategoriaDonante.SOSTENEDOR);
        assertThat(p.categoria()).isEqualTo(CategoriaDonante.SOSTENEDOR);
    }

    @Test
    void elProgresoIndicaCuantoFaltaParaLaMisionActual() {
        PerfilDonante p = perfil();
        CatalogoMisiones catalogo = CatalogoMisiones.predeterminado();
        HistorialDonante h = new HistorialDonante(List.of(donacion(NOV, "Alimentos", 3)), ZoneOffset.UTC);
        new MotorIncentivos(catalogo).evaluar(p, h, NOV, AHORA);

        MotorIncentivos.Progreso progreso = new MotorIncentivos(catalogo).progresoActual(p, h, NOV).orElseThrow();
        assertThat(progreso.mision().id()).isEqualTo("COL-2");
        assertThat(progreso.actual()).isZero();
        assertThat(progreso.restante()).isEqualTo(progreso.mision().objetivo());
    }

    @Test
    void elRankingMensualOrdenaPorMisionesCumplidasEnElMes() {
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        UUID caro = UUID.randomUUID();
        List<InsigniaObtenida> delMes = List.of(
                InsigniaObtenida.nueva(beto, "COL-1", "x", "x", CategoriaDonante.COLABORADOR, AHORA),
                InsigniaObtenida.nueva(ana, "COL-1", "x", "x", CategoriaDonante.COLABORADOR, AHORA),
                InsigniaObtenida.nueva(ana, "COL-2", "x", "x", CategoriaDonante.COLABORADOR, AHORA),
                InsigniaObtenida.nueva(caro, "SOS-1", "x", "x", CategoriaDonante.SOSTENEDOR, AHORA),
                InsigniaObtenida.nueva(caro, "SOS-2", "x", "x", CategoriaDonante.SOSTENEDOR, AHORA),
                InsigniaObtenida.nueva(caro, "SOS-3", "x", "x", CategoriaDonante.SOSTENEDOR, AHORA));

        RankingMensual r = CalculadorRanking.calcular(NOV, delMes, Map.of(ana, "Ana", beto, "Beto", caro, "Caro"), AHORA);

        assertThat(r.entradas()).extracting(RankingMensual.Entrada::nombre).containsExactly("Caro", "Ana", "Beto");
        assertThat(r.podio()).hasSize(3);
        assertThat(r.entradas().get(0).misionesCumplidas()).isEqualTo(3);
    }
}
