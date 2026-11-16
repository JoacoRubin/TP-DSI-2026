package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;

/** Puertos de salida de la aplicación de Incentivos. */
public final class Puertos {

    private Puertos() {
    }

    public interface Notificador {
        void notificar(Destinatario destinatario, String asunto, String mensaje, String tipo);
    }

    /**
     * Dispara el flujo de automatización low-code (n8n) que genera la imagen de la insignia y la
     * publica en redes sociales mencionando al donante.
     */
    public interface DifusorLogros {
        void difundir(PerfilDonante perfil, InsigniaObtenida insignia, String texto);
    }
}
