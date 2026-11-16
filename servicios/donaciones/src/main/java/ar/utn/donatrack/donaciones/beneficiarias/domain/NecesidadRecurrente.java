package ar.utn.donatrack.donaciones.beneficiarias.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Consumo habitual: el objetivo se renueva en cada período (semanal / mensual). */
public class NecesidadRecurrente extends Necesidad {

    public record Recepcion(LocalDate fecha, int cantidad) {
    }

    private final Periodicidad periodicidad;
    private final List<Recepcion> recepciones;

    public NecesidadRecurrente(UUID id, UUID entidadId, UUID subcategoriaId, String subcategoriaNombre,
                               String descripcion, int cantidadRequerida, String unidad, int cantidadComprometida,
                               Periodicidad periodicidad, List<Recepcion> recepciones, boolean activa, Instant creadaEn) {
        super(id, entidadId, subcategoriaId, subcategoriaNombre, descripcion, cantidadRequerida, unidad,
                cantidadComprometida, activa, creadaEn);
        this.periodicidad = Objects.requireNonNull(periodicidad, "periodicidad");
        this.recepciones = new ArrayList<>(recepciones == null ? List.of() : recepciones);
    }

    public static NecesidadRecurrente nueva(UUID id, UUID entidadId, UUID subcategoriaId, String subcategoriaNombre,
                                            String descripcion, int cantidad, String unidad, Periodicidad periodicidad,
                                            Instant ahora) {
        return new NecesidadRecurrente(id, entidadId, subcategoriaId, subcategoriaNombre, descripcion, cantidad, unidad,
                0, periodicidad, List.of(), true, ahora);
    }

    @Override
    public TipoNecesidad tipo() {
        return TipoNecesidad.RECURRENTE;
    }

    @Override
    public int cantidadRecibida(LocalDate hoy) {
        return recepciones.stream()
                .filter(r -> periodicidad.mismoPeriodo(r.fecha(), hoy))
                .mapToInt(Recepcion::cantidad)
                .sum();
    }

    @Override
    protected void acumularRecepcion(int cantidad, LocalDate fecha) {
        recepciones.add(new Recepcion(fecha, cantidad));
    }

    public Periodicidad periodicidad() { return periodicidad; }
    public List<Recepcion> recepciones() { return List.copyOf(recepciones); }
}
