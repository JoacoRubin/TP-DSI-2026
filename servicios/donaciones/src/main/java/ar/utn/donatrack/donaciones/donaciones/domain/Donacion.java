package ar.utn.donatrack.donaciones.donaciones.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Donación resultante de la segmentación: SIEMPRE asociada a una única subcategoría
 * (unidad mínima de asignación). Aggregate root: todo cambio de estado pasa por acá.
 */
public class Donacion {

    private final UUID id;
    private final UUID cargaId;
    private final UUID donanteId;
    private String descripcionGeneral;
    private final UUID subcategoriaId;
    private final String subcategoriaNombre;
    private final String categoriaNombre;
    private final String unidad;
    private final LocalDate fechaVencimiento;
    private final EstadoUso estadoUso;
    private final List<Bien> bienes;
    private final double pesoKg;
    private final double volumenM3;
    private EstadoDonacion estado;
    private UUID necesidadId;
    private UUID entidadId;
    private String patenteEntrega;
    private Instant entregadaEn;
    private final Instant registradaEn;
    private final List<CambioEstado> historial;

    public Donacion(UUID id, UUID cargaId, UUID donanteId, String descripcionGeneral, UUID subcategoriaId,
                    String subcategoriaNombre, String categoriaNombre, String unidad, LocalDate fechaVencimiento,
                    EstadoUso estadoUso, List<Bien> bienes, double pesoKg, double volumenM3, EstadoDonacion estado,
                    UUID necesidadId, UUID entidadId, String patenteEntrega, Instant entregadaEn, Instant registradaEn,
                    List<CambioEstado> historial) {
        this.id = Objects.requireNonNull(id);
        this.cargaId = cargaId;
        this.donanteId = Objects.requireNonNull(donanteId);
        this.descripcionGeneral = descripcionGeneral;
        this.subcategoriaId = Objects.requireNonNull(subcategoriaId);
        this.subcategoriaNombre = subcategoriaNombre;
        this.categoriaNombre = categoriaNombre;
        this.unidad = unidad;
        this.fechaVencimiento = fechaVencimiento;
        this.estadoUso = estadoUso;
        if (bienes == null || bienes.isEmpty()) {
            throw new ReglaDeNegocioException("Una donación debe contener al menos un bien");
        }
        this.bienes = List.copyOf(bienes);
        this.pesoKg = pesoKg;
        this.volumenM3 = volumenM3;
        this.estado = Objects.requireNonNull(estado);
        this.necesidadId = necesidadId;
        this.entidadId = entidadId;
        this.patenteEntrega = patenteEntrega;
        this.entregadaEn = entregadaEn;
        this.registradaEn = registradaEn;
        this.historial = new ArrayList<>(historial == null ? List.of() : historial);
    }

    public static Donacion registrar(UUID id, UUID cargaId, UUID donanteId, String descripcionGeneral,
                                     UUID subcategoriaId, String subcategoriaNombre, String categoriaNombre,
                                     String unidad, LocalDate fechaVencimiento, EstadoUso estadoUso, List<Bien> bienes,
                                     double pesoKg, double volumenM3, String responsable, Instant ahora) {
        Donacion d = new Donacion(id, cargaId, donanteId, descripcionGeneral, subcategoriaId, subcategoriaNombre,
                categoriaNombre, unidad, fechaVencimiento, estadoUso, bienes, pesoKg, volumenM3,
                EstadoDonacion.EN_DEPOSITO, null, null, null, null, ahora, List.of());
        d.historial.add(new CambioEstado(null, EstadoDonacion.EN_DEPOSITO, ahora, responsable, "Ingreso al depósito"));
        return d;
    }

    // ---------------------------------------------------------------- transiciones

    public void asignarA(UUID necesidadId, UUID entidadId, String responsable, Instant ahora) {
        transicionar(EstadoDonacion.ASIGNADA, ahora, responsable, null);
        this.necesidadId = Objects.requireNonNull(necesidadId);
        this.entidadId = Objects.requireNonNull(entidadId);
    }

    public void marcarListaParaEntregar(Instant ahora) {
        transicionar(EstadoDonacion.LISTA_PARA_ENTREGAR, ahora, "logistica", "Incluida en una ruta planificada");
    }

    public void iniciarTraslado(String patente, Instant ahora) {
        transicionar(EstadoDonacion.EN_TRASLADO, ahora, "logistica", "Camión " + patente + " inició el recorrido");
        this.patenteEntrega = patente;
    }

    public void confirmarEntrega(String patente, Instant ahora) {
        transicionar(EstadoDonacion.ENTREGADA, ahora, "entidad", "Recepción confirmada por la entidad beneficiaria");
        this.patenteEntrega = patente;
        this.entregadaEn = ahora;
    }

    public void registrarEntregaFallida(String motivo, Instant ahora) {
        String justificacion = Textos.requerido(motivo, "justificación de la entrega fallida");
        transicionar(EstadoDonacion.ENTREGA_FALLIDA, ahora, "logistica", justificacion);
    }

    /** Vuelve a quedar disponible para un nuevo matchmaking: se libera la asignación. */
    public void devolverADeposito(String responsable, String motivo, Instant ahora) {
        transicionar(EstadoDonacion.EN_DEPOSITO, ahora, responsable, motivo);
        this.necesidadId = null;
        this.entidadId = null;
        this.patenteEntrega = null;
    }

    public void marcarVencida(String responsable, String motivo, Instant ahora) {
        transicionar(EstadoDonacion.VENCIDA, ahora, responsable, motivo == null ? "Bienes vencidos" : motivo);
    }

    public void actualizarDescripcion(String descripcion) {
        this.descripcionGeneral = descripcion;
    }

    private void transicionar(EstadoDonacion destino, Instant ahora, String responsable, String motivo) {
        if (!estado.puedePasarA(destino)) {
            throw new ReglaDeNegocioException("La donación no puede pasar de '" + estado.etiqueta() + "' a '"
                    + destino.etiqueta() + "'");
        }
        historial.add(new CambioEstado(estado, destino, ahora, responsable, motivo));
        this.estado = destino;
    }

    // ---------------------------------------------------------------- consultas

    public int cantidadTotal() {
        return bienes.stream().mapToInt(Bien::cantidad).sum();
    }

    public boolean puedeEliminarse() {
        return estado == EstadoDonacion.EN_DEPOSITO;
    }

    public boolean tieneVencimientoAnteriorA(LocalDate fecha) {
        return fechaVencimiento != null && fechaVencimiento.isBefore(fecha);
    }

    public String descripcionCorta() {
        int cantidad = cantidadTotal();
        String u = unidad == null ? null : (cantidad == 1 ? singular(unidad) : unidad);
        return cantidad + " " + (u == null ? "" : u + " de ") + subcategoriaNombre;
    }

    /** "unidades" → "unidad", "pares" → "par", "paquetes" → "paquete"; abreviaturas ("kg") sin cambios. */
    static String singular(String plural) {
        if (plural.matches(".*[dnrl]es")) {
            return plural.substring(0, plural.length() - 2);
        }
        return plural.endsWith("s") ? plural.substring(0, plural.length() - 1) : plural;
    }

    public UUID id() { return id; }
    public UUID cargaId() { return cargaId; }
    public UUID donanteId() { return donanteId; }
    public String descripcionGeneral() { return descripcionGeneral; }
    public UUID subcategoriaId() { return subcategoriaId; }
    public String subcategoriaNombre() { return subcategoriaNombre; }
    public String categoriaNombre() { return categoriaNombre; }
    public String unidad() { return unidad; }
    public LocalDate fechaVencimiento() { return fechaVencimiento; }
    public EstadoUso estadoUso() { return estadoUso; }
    public List<Bien> bienes() { return bienes; }
    public double pesoKg() { return pesoKg; }
    public double volumenM3() { return volumenM3; }
    public EstadoDonacion estado() { return estado; }
    public UUID necesidadId() { return necesidadId; }
    public UUID entidadId() { return entidadId; }
    public String patenteEntrega() { return patenteEntrega; }
    public Instant entregadaEn() { return entregadaEn; }
    public Instant registradaEn() { return registradaEn; }
    public List<CambioEstado> historial() { return List.copyOf(historial); }
}
