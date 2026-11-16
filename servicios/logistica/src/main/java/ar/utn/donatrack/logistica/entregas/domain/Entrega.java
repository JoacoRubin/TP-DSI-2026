package ar.utn.donatrack.logistica.entregas.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.flota.domain.Camion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Entrega de UNA donación asignada a su entidad beneficiaria. */
public class Entrega {

    private final UUID id;
    private final UUID donacionId;
    private final UUID donanteId;
    private final String donanteNombre;
    private final UUID entidadId;
    private final Destino destino;
    private final DetalleDonacion detalle;
    private final double pesoKg;
    private final double volumenM3;
    private EstadoEntrega estado;
    private UUID rutaId;
    private UUID camionId;
    private String patente;
    private final List<String> fotos;
    private String motivoNoRecepcion;
    private Instant entregadaEn;
    private final Instant creadaEn;
    private final List<RegistroEstado> historial;

    public Entrega(UUID id, UUID donacionId, UUID donanteId, String donanteNombre, UUID entidadId, Destino destino,
                   DetalleDonacion detalle, double pesoKg, double volumenM3, EstadoEntrega estado, UUID rutaId,
                   UUID camionId, String patente, List<String> fotos, String motivoNoRecepcion, Instant entregadaEn,
                   Instant creadaEn, List<RegistroEstado> historial) {
        this.id = Objects.requireNonNull(id);
        this.donacionId = Objects.requireNonNull(donacionId);
        this.donanteId = donanteId;
        this.donanteNombre = donanteNombre;
        this.entidadId = Objects.requireNonNull(entidadId);
        this.destino = Objects.requireNonNull(destino);
        this.detalle = detalle;
        this.pesoKg = pesoKg;
        this.volumenM3 = volumenM3;
        this.estado = Objects.requireNonNull(estado);
        this.rutaId = rutaId;
        this.camionId = camionId;
        this.patente = patente;
        this.fotos = new ArrayList<>(fotos == null ? List.of() : fotos);
        this.motivoNoRecepcion = motivoNoRecepcion;
        this.entregadaEn = entregadaEn;
        this.creadaEn = creadaEn;
        this.historial = new ArrayList<>(historial == null ? List.of() : historial);
    }

    public static Entrega nueva(UUID id, UUID donacionId, UUID donanteId, String donanteNombre, UUID entidadId,
                                Destino destino, DetalleDonacion detalle, double pesoKg, double volumenM3, Instant ahora) {
        Entrega e = new Entrega(id, donacionId, donanteId, donanteNombre, entidadId, destino, detalle,
                Math.max(pesoKg, 0.1), Math.max(volumenM3, 0.001), EstadoEntrega.PENDIENTE, null, null, null,
                List.of(), null, null, ahora, List.of());
        e.historial.add(new RegistroEstado(EstadoEntrega.PENDIENTE, ahora, "Asignación recibida de Donaciones"));
        return e;
    }

    public void enviarAPlanificar(Instant ahora) {
        transicionar(EstadoEntrega.EN_PLANIFICACION, ahora, "Enviada al planificador de rutas");
    }

    public void planificar(UUID rutaId, Camion camion, Instant ahora) {
        transicionar(EstadoEntrega.PLANIFICADA, ahora, "Asignada al camión " + camion.patente());
        this.rutaId = rutaId;
        this.camionId = camion.id();
        this.patente = camion.patente();
    }

    /** El planificador no pudo ubicarla en ninguna ruta: vuelve a la cola para replanificar. */
    public void devolverAPendiente(String motivo, Instant ahora) {
        transicionar(EstadoEntrega.PENDIENTE, ahora, motivo);
        this.rutaId = null;
        this.camionId = null;
        this.patente = null;
    }

    public void iniciarTraslado(Instant ahora) {
        transicionar(EstadoEntrega.EN_TRASLADO, ahora, "El camión " + patente + " inició el recorrido");
    }

    public void confirmarRecepcion(List<String> nuevasFotos, Instant ahora) {
        transicionar(EstadoEntrega.ENTREGADA, ahora, "Recepción confirmada por la entidad");
        this.entregadaEn = ahora;
        agregarFotos(nuevasFotos);
    }

    public void agregarFotos(List<String> nuevasFotos) {
        if (nuevasFotos != null) {
            fotos.addAll(nuevasFotos);
        }
    }

    public void informarNoRecibida(String motivo, Instant ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("Indicá el motivo por el que no se recibió la entrega");
        }
        transicionar(EstadoEntrega.NO_RECIBIDA, ahora, motivo.trim());
        this.motivoNoRecepcion = motivo.trim();
    }

    public void reprogramar(Instant ahora) {
        devolverAPendiente("Reprogramada por la administración", ahora);
        this.motivoNoRecepcion = null;
    }

    public void cancelar(String motivo, Instant ahora) {
        transicionar(EstadoEntrega.CANCELADA, ahora, motivo);
    }

    public boolean puedeCancelarse() {
        return estado.siguientes().contains(EstadoEntrega.CANCELADA);
    }

    private void transicionar(EstadoEntrega destino, Instant ahora, String detalle) {
        if (!estado.siguientes().contains(destino)) {
            throw new ReglaDeNegocioException("La entrega no puede pasar de '" + estado.etiqueta() + "' a '"
                    + destino.etiqueta() + "'");
        }
        this.estado = destino;
        historial.add(new RegistroEstado(destino, ahora, detalle));
    }

    public UUID id() { return id; }
    public UUID donacionId() { return donacionId; }
    public UUID donanteId() { return donanteId; }
    public String donanteNombre() { return donanteNombre; }
    public UUID entidadId() { return entidadId; }
    public Destino destino() { return destino; }
    public DetalleDonacion detalle() { return detalle; }
    public double pesoKg() { return pesoKg; }
    public double volumenM3() { return volumenM3; }
    public EstadoEntrega estado() { return estado; }
    public UUID rutaId() { return rutaId; }
    public UUID camionId() { return camionId; }
    public String patente() { return patente; }
    public List<String> fotos() { return List.copyOf(fotos); }
    public String motivoNoRecepcion() { return motivoNoRecepcion; }
    public Instant entregadaEn() { return entregadaEn; }
    public Instant creadaEn() { return creadaEn; }
    public List<RegistroEstado> historial() { return List.copyOf(historial); }
}
