package ar.utn.donatrack.logistica.entregas.application;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;

import java.time.Clock;
import java.util.UUID;

/** La donación venció o volvió al depósito: su entrega deja de planificarse. */
public class CancelarEntrega {

    private final EntregaRepositorio entregas;
    private final Clock reloj;

    public CancelarEntrega(EntregaRepositorio entregas, Clock reloj) {
        this.entregas = entregas;
        this.reloj = reloj;
    }

    public void ejecutar(UUID donacionId, String motivo) {
        entregas.buscarPorDonacion(donacionId).ifPresent(e -> {
            if (e.estado().esFinal() && !e.puedeCancelarse()) {
                return;
            }
            if (!e.puedeCancelarse()) {
                throw new ReglaDeNegocioException("La entrega está en curso y no puede cancelarse");
            }
            e.cancelar(motivo == null ? "Cancelada desde Donaciones" : motivo, reloj.instant());
            entregas.guardar(e);
        });
    }
}
