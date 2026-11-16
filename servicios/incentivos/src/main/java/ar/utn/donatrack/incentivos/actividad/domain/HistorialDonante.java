package ar.utn.donatrack.incentivos.actividad.domain;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Métricas calculadas sobre la actividad de UN donante. Value Object inmutable. */
public class HistorialDonante {

    private final List<ActividadDonacion> actividades;
    private final ZoneId zona;

    public HistorialDonante(List<ActividadDonacion> actividades, ZoneId zona) {
        this.actividades = List.copyOf(actividades);
        this.zona = zona;
    }

    private YearMonth mesDe(ActividadDonacion a) {
        return YearMonth.from(a.registradaEn().atZone(zona));
    }

    private Set<YearMonth> mesesConDonacion() {
        return actividades.stream().map(this::mesDe).collect(Collectors.toSet());
    }

    /**
     * Meses consecutivos con al menos una donación. Si el mes en curso todavía no tiene
     * donaciones, la racha se cuenta desde el mes anterior (aún puede continuarla); si el mes
     * anterior tampoco tiene, la racha se perdió.
     */
    public int rachaMeses(YearMonth mesActual) {
        Set<YearMonth> meses = mesesConDonacion();
        YearMonth cursor = meses.contains(mesActual) ? mesActual : mesActual.minusMonths(1);
        int racha = 0;
        while (meses.contains(cursor)) {
            racha++;
            cursor = cursor.minusMonths(1);
        }
        return racha;
    }

    public int categoriasDistintas() {
        return (int) actividades.stream().map(ActividadDonacion::categoria).filter(Objects::nonNull).distinct().count();
    }

    /** Mayor cantidad de bienes entregados en una misma carga al depósito. */
    public int mayorCarga() {
        return actividades.stream()
                .collect(Collectors.groupingBy(a -> a.cargaId() == null ? a.donacionId() : a.cargaId(),
                        Collectors.summingInt(ActividadDonacion::cantidad)))
                .values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public int donacionesExitosas() {
        return (int) actividades.stream().filter(ActividadDonacion::entregada).count();
    }

    public int totalDonaciones() {
        return actividades.size();
    }

    public int totalBienes() {
        return actividades.stream().mapToInt(ActividadDonacion::cantidad).sum();
    }

    public int organizacionesAyudadas() {
        return (int) actividades.stream().filter(ActividadDonacion::entregada).map(ActividadDonacion::entidadId)
                .filter(Objects::nonNull).distinct().count();
    }

    public record Periodo(YearMonth mes, int donaciones, int bienes) {
    }

    /** Evolución de los últimos N meses (incluye meses en cero, para graficar). */
    public List<Periodo> evolucion(YearMonth hasta, int meses) {
        Map<YearMonth, List<ActividadDonacion>> porMes = actividades.stream().collect(Collectors.groupingBy(this::mesDe));
        List<Periodo> r = new ArrayList<>();
        for (int i = meses - 1; i >= 0; i--) {
            YearMonth m = hasta.minusMonths(i);
            List<ActividadDonacion> del = porMes.getOrDefault(m, List.of());
            r.add(new Periodo(m, del.size(), del.stream().mapToInt(ActividadDonacion::cantidad).sum()));
        }
        return r;
    }

    public UUID donanteId() {
        return actividades.isEmpty() ? null : actividades.get(0).donanteId();
    }
}
