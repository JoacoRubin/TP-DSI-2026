package ar.utn.donatrack.notificaciones.envios.infrastructure.persistencia;

import ar.utn.donatrack.notificaciones.envios.domain.EstadoNotificacion;
import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import ar.utn.donatrack.notificaciones.envios.domain.NotificacionRepositorio;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface NotificacionJpaRepository extends JpaRepository<NotificacionJpa, UUID> {

    List<NotificacionJpa> findByEstadoAndIntentosLessThanOrderByCreadaEnAsc(EstadoNotificacion estado, int intentos,
                                                                             Pageable pagina);

    List<NotificacionJpa> findByDestinatarioEmailOrderByCreadaEnDesc(String email, Pageable pagina);
}

@Repository
class RepositorioNotificacionesJpa implements NotificacionRepositorio {

    private final NotificacionJpaRepository jpa;

    RepositorioNotificacionesJpa(NotificacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Notificacion> buscar(UUID id) {
        return jpa.findById(id).map(NotificacionJpa::aDominio);
    }

    @Override
    public void guardar(Notificacion notificacion) {
        jpa.save(NotificacionJpa.desde(notificacion));
    }

    @Override
    public List<Notificacion> reintentables(int maximo) {
        return jpa.findByEstadoAndIntentosLessThanOrderByCreadaEnAsc(EstadoNotificacion.FALLIDA, Notificacion.MAX_INTENTOS,
                PageRequest.of(0, maximo)).stream().map(NotificacionJpa::aDominio).toList();
    }

    @Override
    public List<Notificacion> deDestinatario(String email, int limite) {
        return jpa.findByDestinatarioEmailOrderByCreadaEnDesc(email, PageRequest.of(0, limite)).stream()
                .map(NotificacionJpa::aDominio).toList();
    }
}
