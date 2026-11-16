package ar.utn.donatrack.donaciones.asignacion.application;

import ar.utn.donatrack.donaciones.asignacion.domain.ContextoMatchmaking;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;

/**
 * Proceso batch: evalúa TODAS las donaciones en depósito. Se dispara en horario de baja carga
 * (cron) o a demanda (asincrónico). Ver justificación "tiempo real vs batch" en docs.
 */
public class EjecutarMatchmaking {

    private final EvaluadorDonaciones evaluador;
    private final DonacionRepositorio donaciones;

    public EjecutarMatchmaking(EvaluadorDonaciones evaluador, DonacionRepositorio donaciones) {
        this.evaluador = evaluador;
        this.donaciones = donaciones;
    }

    public int ejecutar() {
        ContextoMatchmaking contexto = evaluador.contexto();
        int evaluadas = 0;
        for (Donacion d : donaciones.enEstado(EstadoDonacion.EN_DEPOSITO)) {
            evaluador.evaluar(d, contexto);
            evaluadas++;
        }
        return evaluadas;
    }
}
