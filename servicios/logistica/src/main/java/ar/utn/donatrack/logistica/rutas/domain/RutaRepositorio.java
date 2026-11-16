package ar.utn.donatrack.logistica.rutas.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RutaRepositorio {

    Optional<Ruta> buscar(UUID id);

    void guardar(Ruta ruta);

    /** Actualización parcial y atómica de la posición (no reescribe el documento completo). */
    void guardarPosicion(Ruta ruta);

    List<Ruta> enEstados(List<EstadoRuta> estados);

    List<Ruta> activasDeDonante(UUID donanteId);

    List<Ruta> activasDeEntidad(UUID entidadId);

    List<Ruta> dePatente(String patente, List<EstadoRuta> estados);

    List<Ruta> recientes(int limite);
}
