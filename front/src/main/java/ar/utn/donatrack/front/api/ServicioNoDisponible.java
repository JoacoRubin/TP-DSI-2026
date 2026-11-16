package ar.utn.donatrack.front.api;

public class ServicioNoDisponible extends RuntimeException {
    public ServicioNoDisponible(String servicio, Throwable causa) {
        super("El servicio de " + servicio + " no está disponible en este momento. Intentá nuevamente en unos minutos.", causa);
    }
}
