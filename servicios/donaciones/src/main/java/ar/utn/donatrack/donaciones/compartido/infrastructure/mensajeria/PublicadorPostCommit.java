package ar.utn.donatrack.donaciones.compartido.infrastructure.mensajeria;

import ar.utn.donatrack.soporte.mensajeria.PublicadorMensajes;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Si hay una transacción activa, el mensaje se publica recién DESPUÉS del commit.
 * Evita "mensajes fantasma" (avisar algo que luego se revirtió). Limitación conocida: si el
 * proceso cae entre el commit y el envío, el mensaje se pierde → la solución completa es el
 * patrón Transactional Outbox (propuesto como bonus en la Entrega 6).
 */
@Component
public class PublicadorPostCommit {

    private final PublicadorMensajes publicador;

    public PublicadorPostCommit(PublicadorMensajes publicador) {
        this.publicador = publicador;
    }

    public void publicar(String cola, Object mensaje) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicador.publicar(cola, mensaje);
                }
            });
        } else {
            publicador.publicar(cola, mensaje);
        }
    }
}
