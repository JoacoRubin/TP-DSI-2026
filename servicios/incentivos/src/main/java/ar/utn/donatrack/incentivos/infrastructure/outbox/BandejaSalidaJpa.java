package ar.utn.donatrack.incentivos.infrastructure.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

interface MensajesSalientesJpa extends JpaRepository<MensajeSaliente, Long> {

    List<MensajeSaliente> findByEstadoOrderByIdAsc(EstadoMensaje estado, PageRequest pagina);
}

/**
 * Sin {@code @Transactional} propio a propósito: {@link #guardar} se une a la transacción del
 * caso de uso (eso es lo que hace atómico al outbox) y, fuera de una, cada save es la suya.
 */
@Repository
class BandejaSalidaJpa implements BandejaSalida {

    private final MensajesSalientesJpa jpa;

    BandejaSalidaJpa(MensajesSalientesJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public void guardar(MensajeSaliente mensaje) {
        jpa.save(mensaje);
    }

    @Override
    public List<MensajeSaliente> pendientes(int limite) {
        return jpa.findByEstadoOrderByIdAsc(EstadoMensaje.PENDIENTE, PageRequest.of(0, limite));
    }
}
