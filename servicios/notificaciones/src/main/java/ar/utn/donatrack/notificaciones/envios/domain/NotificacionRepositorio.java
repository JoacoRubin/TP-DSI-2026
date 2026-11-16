package ar.utn.donatrack.notificaciones.envios.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepositorio {

    Optional<Notificacion> buscar(UUID id);

    void guardar(Notificacion notificacion);

    List<Notificacion> reintentables(int maximo);

    List<Notificacion> deDestinatario(String email, int limite);
}
