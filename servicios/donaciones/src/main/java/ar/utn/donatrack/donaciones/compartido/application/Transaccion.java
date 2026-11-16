package ar.utn.donatrack.donaciones.compartido.application;

import java.util.function.Supplier;

/** Puerto: unidad de trabajo. Permite a un caso de uso delimitar transacciones sin conocer Spring. */
public interface Transaccion {

    <T> T ejecutar(Supplier<T> trabajo);

    default void ejecutar(Runnable trabajo) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        });
    }
}
