package ar.utn.donatrack.incentivos.infrastructure.outbox;

import java.util.List;

/** Tabla outbox: se escribe dentro de la transacción del caso de uso y se lee al despachar. */
public interface BandejaSalida {

    void guardar(MensajeSaliente mensaje);

    /** Pendientes en orden de inserción. */
    List<MensajeSaliente> pendientes(int limite);
}
