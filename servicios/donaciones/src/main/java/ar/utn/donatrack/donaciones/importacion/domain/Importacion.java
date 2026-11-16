package ar.utn.donatrack.donaciones.importacion.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Proceso de importación masiva (job). Permite consultar el progreso mientras corre. */
public class Importacion {

    public enum Estado { EN_CURSO, FINALIZADA, FALLIDA }

    public record ErrorFila(int linea, String mensaje) {
    }

    public static final int MAX_ERRORES_REGISTRADOS = 200;

    private final UUID id;
    private final String archivo;
    private Estado estado;
    private int filasProcesadas;
    private int creados;
    private int actualizados;
    private int conError;
    private final List<ErrorFila> errores;
    private final Instant iniciadaEn;
    private Instant finalizadaEn;
    private String detalleFallo;

    public Importacion(UUID id, String archivo, Estado estado, int filasProcesadas, int creados, int actualizados,
                       int conError, List<ErrorFila> errores, Instant iniciadaEn, Instant finalizadaEn, String detalleFallo) {
        this.id = Objects.requireNonNull(id);
        this.archivo = archivo;
        this.estado = estado;
        this.filasProcesadas = filasProcesadas;
        this.creados = creados;
        this.actualizados = actualizados;
        this.conError = conError;
        this.errores = new ArrayList<>(errores == null ? List.of() : errores);
        this.iniciadaEn = iniciadaEn;
        this.finalizadaEn = finalizadaEn;
        this.detalleFallo = detalleFallo;
    }

    public static Importacion iniciar(UUID id, String archivo, Instant ahora) {
        return new Importacion(id, archivo, Estado.EN_CURSO, 0, 0, 0, 0, List.of(), ahora, null, null);
    }

    public void registrarLote(int creados, int actualizados, List<ErrorFila> erroresLote, int filas) {
        this.creados += creados;
        this.actualizados += actualizados;
        this.conError += erroresLote.size();
        this.filasProcesadas += filas;
        for (ErrorFila e : erroresLote) {
            if (errores.size() < MAX_ERRORES_REGISTRADOS) {
                errores.add(e);
            }
        }
    }

    public void finalizar(Instant ahora) {
        this.estado = Estado.FINALIZADA;
        this.finalizadaEn = ahora;
    }

    public void fallar(String detalle, Instant ahora) {
        this.estado = Estado.FALLIDA;
        this.detalleFallo = detalle;
        this.finalizadaEn = ahora;
    }

    public UUID id() { return id; }
    public String archivo() { return archivo; }
    public Estado estado() { return estado; }
    public int filasProcesadas() { return filasProcesadas; }
    public int creados() { return creados; }
    public int actualizados() { return actualizados; }
    public int conError() { return conError; }
    public List<ErrorFila> errores() { return List.copyOf(errores); }
    public Instant iniciadaEn() { return iniciadaEn; }
    public Instant finalizadaEn() { return finalizadaEn; }
    public String detalleFallo() { return detalleFallo; }
}
