package ar.utn.donatrack.logistica.rutas.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.flota.domain.Camion;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Ruta del día para un camión: lista ordenada de paradas y su seguimiento en tiempo real. */
public class Ruta {

    /** Se guarda sólo el tramo reciente: el documento no debe crecer sin límite. */
    public static final int MAX_RECORRIDO = 300;
    private static final Duration TOLERANCIA_RELOJ = Duration.ofMinutes(2);

    private final UUID id;
    private final LocalDate fecha;
    private final UUID camionId;
    private final String patente;
    private final List<Parada> paradas;
    private EstadoRuta estado;
    private Instant iniciadaEn;
    private Instant finalizadaEn;
    private Posicion ultimaPosicion;
    private final List<Posicion> recorrido;
    private final Instant creadaEn;

    public Ruta(UUID id, LocalDate fecha, UUID camionId, String patente, List<Parada> paradas, EstadoRuta estado,
                Instant iniciadaEn, Instant finalizadaEn, Posicion ultimaPosicion, List<Posicion> recorrido,
                Instant creadaEn) {
        this.id = Objects.requireNonNull(id);
        this.fecha = fecha;
        this.camionId = camionId;
        this.patente = patente;
        this.paradas = List.copyOf(paradas);
        this.estado = estado;
        this.iniciadaEn = iniciadaEn;
        this.finalizadaEn = finalizadaEn;
        this.ultimaPosicion = ultimaPosicion;
        this.recorrido = new ArrayList<>(recorrido == null ? List.of() : recorrido);
        this.creadaEn = creadaEn;
    }

    public static Ruta planificar(UUID id, LocalDate fecha, Camion camion, List<Parada> paradas, Instant ahora) {
        if (paradas.isEmpty()) {
            throw new ReglaDeNegocioException("Una ruta debe tener al menos una parada");
        }
        return new Ruta(id, fecha, camion.id(), camion.patente(), paradas, EstadoRuta.PLANIFICADA, null, null, null,
                List.of(), ahora);
    }

    public void iniciar(Instant ahora) {
        if (estado != EstadoRuta.PLANIFICADA) {
            throw new ReglaDeNegocioException("La ruta ya fue iniciada o finalizada");
        }
        estado = EstadoRuta.EN_CURSO;
        iniciadaEn = ahora;
    }

    /**
     * @return false si el reporte llegó fuera de orden (se descarta sin error: las redes
     *         móviles reordenan paquetes); excepción si es inválido.
     */
    public boolean registrarPosicion(Posicion p, Instant ahora) {
        if (estado != EstadoRuta.EN_CURSO) {
            throw new ReglaDeNegocioException("Sólo se reciben posiciones de rutas en curso");
        }
        if (p.registradaEn().isAfter(ahora.plus(TOLERANCIA_RELOJ))) {
            throw new ReglaDeNegocioException("La posición tiene una fecha en el futuro");
        }
        if (ultimaPosicion != null && !p.registradaEn().isAfter(ultimaPosicion.registradaEn())) {
            return false;
        }
        ultimaPosicion = p;
        recorrido.add(p);
        if (recorrido.size() > MAX_RECORRIDO) {
            recorrido.subList(0, recorrido.size() - MAX_RECORRIDO).clear();
        }
        return true;
    }

    public void finalizar(Instant ahora) {
        if (estado == EstadoRuta.EN_CURSO) {
            estado = EstadoRuta.FINALIZADA;
            finalizadaEn = ahora;
        }
    }

    public boolean involucraDonante(UUID donanteId) {
        return paradas.stream().flatMap(p -> p.entregas().stream()).anyMatch(e -> donanteId.equals(e.donanteId()));
    }

    public boolean involucraEntidad(UUID entidadId) {
        return paradas.stream().anyMatch(p -> entidadId.equals(p.entidadId()));
    }

    public List<UUID> entregaIds() {
        return paradas.stream().flatMap(p -> p.entregas().stream()).map(Parada.EntregaEnRuta::entregaId).toList();
    }

    public UUID id() { return id; }
    public LocalDate fecha() { return fecha; }
    public UUID camionId() { return camionId; }
    public String patente() { return patente; }
    public List<Parada> paradas() { return paradas; }
    public EstadoRuta estado() { return estado; }
    public Instant iniciadaEn() { return iniciadaEn; }
    public Instant finalizadaEn() { return finalizadaEn; }
    public Posicion ultimaPosicion() { return ultimaPosicion; }
    public List<Posicion> recorrido() { return List.copyOf(recorrido); }
    public Instant creadaEn() { return creadaEn; }
}
