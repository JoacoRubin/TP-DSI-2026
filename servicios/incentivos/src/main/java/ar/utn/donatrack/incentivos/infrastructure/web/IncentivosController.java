package ar.utn.donatrack.incentivos.infrastructure.web;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;
import ar.utn.donatrack.incentivos.misiones.application.ActualizarPreferencias;
import ar.utn.donatrack.incentivos.misiones.application.ConsultasIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.Mision;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.ranking.application.Rankings;
import ar.utn.donatrack.incentivos.ranking.domain.RankingMensual;
import ar.utn.donatrack.soporte.seguridad.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
class IncentivosController {

    private final ConsultasIncentivos consultas;
    private final ActualizarPreferencias preferencias;
    private final Rankings rankings;

    IncentivosController(ConsultasIncentivos consultas, ActualizarPreferencias preferencias, Rankings rankings) {
        this.consultas = consultas;
        this.preferencias = preferencias;
        this.rankings = rankings;
    }

    record MisionVista(String id, String categoria, int orden, String nombre, String descripcion, String tipo,
                       int objetivo, String insignia, String icono, String estado) {
        static MisionVista de(Mision m, String estado) {
            return new MisionVista(m.id(), m.categoria().etiqueta(), m.orden(), m.nombre(), m.descripcion(),
                    m.tipo().name(), m.objetivo(), m.insignia(), m.icono(), estado);
        }
    }

    record InsigniaVista(String misionId, String nombre, String icono, String categoria, Instant obtenidaEn) {
        static InsigniaVista de(InsigniaObtenida i) {
            return new InsigniaVista(i.misionId(), i.nombre(), i.icono(), i.categoria().etiqueta(), i.obtenidaEn());
        }
    }

    record ProgresoVista(MisionVista mision, int actual, int restante, int porcentaje) {
    }

    record PanelVista(UUID donanteId, String nombre, String categoria, String categoriaCodigo, boolean insigniasVisibles,
                      ProgresoVista progreso, List<MisionVista> misiones, List<InsigniaVista> insignias) {
    }

    record PeriodoVista(String mes, int donaciones, int bienes) {
        static PeriodoVista de(HistorialDonante.Periodo p) {
            return new PeriodoVista(p.mes().toString(), p.donaciones(), p.bienes());
        }
    }

    record MetricasVista(int totalDonaciones, int totalBienes, int donacionesExitosas, int organizacionesAyudadas,
                         PeriodoVista mesActual, PeriodoVista mesAnterior, Integer variacionPorcentual,
                         List<PeriodoVista> evolucion, Integer posicionRanking, String categoria, int insignias) {
    }

    record PreferenciasRequest(boolean insigniasVisibles) {
    }

    record PerfilPublicoVista(UUID donanteId, String nombre, String categoria, List<InsigniaVista> insignias) {
    }

    private static void exigirPropioOAdmin(UUID donanteId, Authentication auth) {
        boolean admin = Claims.tieneRol(auth, "ADMIN");
        boolean propio = Claims.referencia(auth).map(r -> r.equals(donanteId.toString())).orElse(false);
        if (!admin && !propio) {
            throw new OperacionNoPermitidaException("Sólo podés consultar tus propios incentivos");
        }
    }

    @GetMapping("/api/donantes/{id}/incentivos")
    PanelVista panel(@PathVariable UUID id, Authentication auth) {
        exigirPropioOAdmin(id, auth);
        ConsultasIncentivos.Panel p = consultas.panel(id);
        ProgresoVista progreso = p.progreso().map(x -> new ProgresoVista(MisionVista.de(x.mision(), "EN_CURSO"),
                x.actual(), x.restante(), x.porcentaje())).orElse(null);
        return new PanelVista(id, p.perfil().nombre(), p.perfil().categoria().etiqueta(), p.perfil().categoria().name(),
                p.perfil().insigniasVisibles(), progreso,
                p.misiones().stream().map(m -> MisionVista.de(m.mision(), m.estado().name())).toList(),
                p.insignias().stream().map(InsigniaVista::de).toList());
    }

    /** Métricas de actividad: período actual y acumulado. */
    @GetMapping("/api/donantes/{id}/metricas")
    MetricasVista metricas(@PathVariable UUID id, Authentication auth) {
        exigirPropioOAdmin(id, auth);
        ConsultasIncentivos.Metricas m = consultas.metricas(id);
        return new MetricasVista(m.totalDonaciones(), m.totalBienes(), m.donacionesExitosas(), m.organizacionesAyudadas(),
                PeriodoVista.de(m.mesActual()), PeriodoVista.de(m.mesAnterior()), m.variacionPorcentual(),
                m.evolucion().stream().map(PeriodoVista::de).toList(), m.posicionRanking(), m.categoria(), m.insignias());
    }

    @GetMapping("/api/donantes/{id}/misiones-completadas")
    List<MisionVista> misionesCompletadas(@PathVariable UUID id, Authentication auth) {
        exigirPropioOAdmin(id, auth);
        return consultas.misionesCompletadas(id).stream().map(m -> MisionVista.de(m, "COMPLETADA")).toList();
    }

    @GetMapping("/api/donantes/{id}/insignias")
    List<InsigniaVista> insignias(@PathVariable UUID id, Authentication auth) {
        exigirPropioOAdmin(id, auth);
        return consultas.insignias(id).stream().map(InsigniaVista::de).toList();
    }

    @PatchMapping("/api/donantes/{id}/preferencias")
    @Transactional
    PanelVista preferencias(@PathVariable UUID id, @RequestBody PreferenciasRequest req, Authentication auth) {
        exigirPropioOAdmin(id, auth);
        preferencias.ejecutar(id, req.insigniasVisibles());
        return panel(id, auth);
    }

    // ------------------------------------------------------------------ rankings (administración)

    @GetMapping("/api/rankings")
    List<RankingMensual> historial() {
        return rankings.historial();
    }

    @GetMapping("/api/rankings/actual")
    RankingMensual actual() {
        return rankings.actual();
    }

    @GetMapping("/api/rankings/{periodo}")
    RankingMensual ranking(@PathVariable String periodo) {
        return rankings.de(YearMonth.parse(periodo));
    }

    @PostMapping("/api/rankings/{periodo}/generacion")
    @Transactional
    RankingMensual generar(@PathVariable String periodo) {
        return rankings.generar(YearMonth.parse(periodo));
    }

    // ------------------------------------------------------------------ público

    /** Los tres primeros, destacados en una sección visible de la aplicación. */
    @GetMapping("/api/publico/podio")
    RankingMensual podio() {
        RankingMensual r = rankings.destacado();
        return new RankingMensual(r.id(), r.anio(), r.mes(), r.podio(), r.generadoEn());
    }

    /** Categoría visible públicamente junto al nombre; insignias sólo si el donante lo permite. */
    @GetMapping("/api/publico/donantes/{id}")
    PerfilPublicoVista perfilPublico(@PathVariable UUID id) {
        PerfilDonante p = consultas.perfil(id);
        return new PerfilPublicoVista(id, p.nombre(), p.categoria().etiqueta(), p.insigniasVisibles()
                ? consultas.insignias(id).stream().map(InsigniaVista::de).toList() : List.of());
    }
}
