package ar.utn.donatrack.notificaciones.envios.application;

import ar.utn.donatrack.notificaciones.envios.domain.Notificacion;
import ar.utn.donatrack.notificaciones.envios.domain.NotificacionRepositorio;
import ar.utn.donatrack.notificaciones.envios.domain.SelectorCanal;

import java.time.Clock;

public class ReintentarFallidas {

    private static final int LOTE = 100;
    private final NotificacionRepositorio repositorio;
    private final SelectorCanal selector;
    private final Clock reloj;

    public ReintentarFallidas(NotificacionRepositorio repositorio, SelectorCanal selector, Clock reloj) {
        this.repositorio = repositorio;
        this.selector = selector;
        this.reloj = reloj;
    }

    public int ejecutar() {
        int procesadas = 0;
        for (Notificacion n : repositorio.reintentables(LOTE)) {
            n.intentarEnvio(selector, reloj.instant());
            repositorio.guardar(n);
            procesadas++;
        }
        return procesadas;
    }
}
