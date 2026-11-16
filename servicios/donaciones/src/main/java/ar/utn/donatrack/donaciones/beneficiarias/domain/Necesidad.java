package ar.utn.donatrack.donaciones.beneficiarias.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Necesidad material de una entidad. Template Method: la lógica común (comprometer, admitir
 * donaciones) vive acá; cada subtipo define cómo se cuenta lo recibido.
 */
public abstract class Necesidad {

    protected final UUID id;
    protected final UUID entidadId;
    protected final UUID subcategoriaId;
    protected final String subcategoriaNombre;
    protected String descripcion;
    protected int cantidadRequerida;
    protected String unidad;
    protected int cantidadComprometida;
    protected boolean activa;
    protected final Instant creadaEn;

    protected Necesidad(UUID id, UUID entidadId, UUID subcategoriaId, String subcategoriaNombre, String descripcion,
                        int cantidadRequerida, String unidad, int cantidadComprometida, boolean activa, Instant creadaEn) {
        this.id = Objects.requireNonNull(id);
        this.entidadId = Objects.requireNonNull(entidadId);
        this.subcategoriaId = Objects.requireNonNull(subcategoriaId, "subcategoría");
        this.subcategoriaNombre = subcategoriaNombre;
        this.descripcion = Textos.requerido(descripcion, "descripción");
        this.cantidadRequerida = validarCantidad(cantidadRequerida);
        this.unidad = unidad == null || unidad.isBlank() ? "unidades" : unidad.trim();
        this.cantidadComprometida = Math.max(0, cantidadComprometida);
        this.activa = activa;
        this.creadaEn = creadaEn;
    }

    private static int validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDeNegocioException("La cantidad requerida debe ser mayor a cero");
        }
        return cantidad;
    }

    public abstract TipoNecesidad tipo();

    /** Cantidad efectivamente recibida que cuenta para satisfacer la necesidad a la fecha. */
    public abstract int cantidadRecibida(LocalDate hoy);

    protected abstract void acumularRecepcion(int cantidad, LocalDate fecha);

    public int cantidadPendiente(LocalDate hoy) {
        return Math.max(0, cantidadRequerida - cantidadRecibida(hoy) - cantidadComprometida);
    }

    public boolean estaSatisfecha(LocalDate hoy) {
        return cantidadRecibida(hoy) >= cantidadRequerida;
    }

    public boolean admiteDonaciones(LocalDate hoy) {
        return activa && cantidadPendiente(hoy) > 0;
    }

    /** Reserva cantidad al confirmarse una asignación (aún no fue entregada). */
    public void comprometer(int cantidad) {
        cantidadComprometida += cantidad;
    }

    public void liberar(int cantidad) {
        cantidadComprometida = Math.max(0, cantidadComprometida - cantidad);
    }

    public void registrarRecepcion(int cantidad, LocalDate fecha) {
        liberar(cantidad);
        acumularRecepcion(cantidad, fecha);
    }

    public void actualizar(String descripcion, int cantidadRequerida, String unidad) {
        this.descripcion = Textos.requerido(descripcion, "descripción");
        this.cantidadRequerida = validarCantidad(cantidadRequerida);
        if (unidad != null && !unidad.isBlank()) {
            this.unidad = unidad.trim();
        }
    }

    public void darDeBaja() {
        this.activa = false;
    }

    public UUID id() { return id; }
    public UUID entidadId() { return entidadId; }
    public UUID subcategoriaId() { return subcategoriaId; }
    public String subcategoriaNombre() { return subcategoriaNombre; }
    public String descripcion() { return descripcion; }
    public int cantidadRequerida() { return cantidadRequerida; }
    public String unidad() { return unidad; }
    public int cantidadComprometida() { return cantidadComprometida; }
    public boolean activa() { return activa; }
    public Instant creadaEn() { return creadaEn; }
}
