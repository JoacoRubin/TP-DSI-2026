package ar.utn.donatrack.contratos;

/**
 * Nombres de las colas del broker. Cada cola tiene un único consumidor (el servicio
 * cuyo nombre aparece primero), por eso se nombran "consumidor.origen".
 */
public final class Colas {
    public static final String NOTIFICACIONES = "notificaciones.solicitudes";
    public static final String DONACIONES_DESDE_LOGISTICA = "donaciones.eventos-logistica";
    public static final String INCENTIVOS_DESDE_DONACIONES = "incentivos.eventos-donaciones";

    private Colas() {
    }
}
