package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.Mision;
import ar.utn.donatrack.incentivos.misiones.domain.MotorIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;
import ar.utn.donatrack.incentivos.ranking.application.Rankings;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConsultasIncentivos {

    public enum EstadoMision { COMPLETADA, EN_CURSO, BLOQUEADA }

    public record MisionConEstado(Mision mision, EstadoMision estado) {
    }

    public record Panel(PerfilDonante perfil, Optional<MotorIncentivos.Progreso> progreso, List<MisionConEstado> misiones,
                        List<InsigniaObtenida> insignias) {
    }

    public record Metricas(int totalDonaciones, int totalBienes, int donacionesExitosas, int organizacionesAyudadas,
                           HistorialDonante.Periodo mesActual, HistorialDonante.Periodo mesAnterior,
                           Integer variacionPorcentual, List<HistorialDonante.Periodo> evolucion,
                           Integer posicionRanking, String categoria, int insignias) {
    }

    private final Repositorios.PerfilRepositorio perfiles;
    private final Repositorios.InsigniaRepositorio insignias;
    private final ActividadRepositorio actividades;
    private final MotorIncentivos motor;
    private final Rankings rankings;
    private final Clock reloj;

    public ConsultasIncentivos(Repositorios.PerfilRepositorio perfiles, Repositorios.InsigniaRepositorio insignias,
                               ActividadRepositorio actividades, MotorIncentivos motor, Rankings rankings, Clock reloj) {
        this.perfiles = perfiles;
        this.insignias = insignias;
        this.actividades = actividades;
        this.motor = motor;
        this.rankings = rankings;
        this.reloj = reloj;
    }

    public PerfilDonante perfil(UUID donanteId) {
        return perfiles.buscar(donanteId).orElseThrow(() -> new RecursoNoEncontradoException("Perfil de incentivos", donanteId));
    }

    private HistorialDonante historial(UUID donanteId) {
        return new HistorialDonante(actividades.deDonante(donanteId), reloj.getZone());
    }

    public Panel panel(UUID donanteId) {
        PerfilDonante p = perfil(donanteId);
        List<InsigniaObtenida> obtenidas = insignias.deDonante(donanteId);
        Set<String> completadas = obtenidas.stream().map(InsigniaObtenida::misionId).collect(Collectors.toSet());
        String actual = p.misionActual(motor.catalogo()).map(Mision::id).orElse(null);
        List<MisionConEstado> misiones = motor.catalogo().misiones().stream()
                .map(m -> new MisionConEstado(m, completadas.contains(m.id()) ? EstadoMision.COMPLETADA
                        : m.id().equals(actual) ? EstadoMision.EN_CURSO : EstadoMision.BLOQUEADA))
                .toList();
        return new Panel(p, motor.progresoActual(p, historial(donanteId), YearMonth.now(reloj)), misiones, obtenidas);
    }

    public Metricas metricas(UUID donanteId) {
        PerfilDonante p = perfil(donanteId);
        HistorialDonante h = historial(donanteId);
        YearMonth mes = YearMonth.now(reloj);
        List<HistorialDonante.Periodo> evolucion = h.evolucion(mes, 12);
        HistorialDonante.Periodo actual = evolucion.get(evolucion.size() - 1);
        HistorialDonante.Periodo anterior = evolucion.get(evolucion.size() - 2);
        Integer variacion = anterior.donaciones() == 0 ? null
                : (int) Math.round(100.0 * (actual.donaciones() - anterior.donaciones()) / anterior.donaciones());
        return new Metricas(h.totalDonaciones(), h.totalBienes(), h.donacionesExitosas(), h.organizacionesAyudadas(),
                actual, anterior, variacion, evolucion, rankings.posicionActual(donanteId).orElse(null),
                p.categoria().etiqueta(), insignias.deDonante(donanteId).size());
    }

    public List<InsigniaObtenida> insignias(UUID donanteId) {
        return insignias.deDonante(donanteId);
    }

    public List<Mision> misionesCompletadas(UUID donanteId) {
        Set<String> ids = insignias.deDonante(donanteId).stream().map(InsigniaObtenida::misionId).collect(Collectors.toSet());
        return motor.catalogo().misiones().stream().filter(m -> ids.contains(m.id())).toList();
    }
}
