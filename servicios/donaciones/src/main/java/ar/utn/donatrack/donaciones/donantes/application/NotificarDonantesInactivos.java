package ar.utn.donatrack.donaciones.donantes.application;

import ar.utn.donatrack.donaciones.compartido.application.Enlaces;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Tarea calendarizada: incentiva a quienes no interactúan hace más de 20 días. */
public class NotificarDonantesInactivos {

    public static final Duration UMBRAL = Duration.ofDays(20);

    private final DonanteRepositorio donantes;
    private final Notificador notificador;
    private final Enlaces enlaces;
    private final Clock reloj;

    public NotificarDonantesInactivos(DonanteRepositorio donantes, Notificador notificador, Enlaces enlaces, Clock reloj) {
        this.donantes = donantes;
        this.notificador = notificador;
        this.enlaces = enlaces;
        this.reloj = reloj;
    }

    public int ejecutar() {
        Instant ahora = reloj.instant();
        int avisados = 0;
        for (Donante d : donantes.sinInteraccionDesde(ahora.minus(UMBRAL))) {
            if (d.requiereAvisoInactividad(ahora, UMBRAL)) {
                notificador.notificar(d.comoDestinatario(), "¡Te extrañamos en DonaTrack!",
                        "Hola " + d.nombreVisible() + ", hace más de 20 días que no sabemos de vos. "
                                + "Hay entidades que necesitan ayuda: acercate al depósito o mirá las necesidades en "
                                + enlaces.baseFront() + "/donante/entidades",
                        "INACTIVIDAD");
                d.registrarAvisoInactividad(ahora);
                donantes.guardar(d);
                avisados++;
            }
        }
        return avisados;
    }
}
