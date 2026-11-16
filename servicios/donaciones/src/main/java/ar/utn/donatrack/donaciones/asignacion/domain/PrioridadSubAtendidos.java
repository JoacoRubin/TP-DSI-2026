package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;

/**
 * S = 1 / (1 + k·n), con n = donaciones recibidas por la entidad en el último trimestre y k un
 * factor de suavizado (ej. 0,1) para que el puntaje baje gradualmente.
 */
public class PrioridadSubAtendidos implements AlgoritmoMatchmaking {

    public static final String NOMBRE = "Prioridad a sub-atendidos";
    private final double k;

    public PrioridadSubAtendidos(double k) {
        if (k <= 0) {
            throw new ReglaDeNegocioException("El factor de suavizado debe ser positivo");
        }
        this.k = k;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public double puntuar(Donacion donacion, Necesidad necesidad, ContextoMatchmaking contexto) {
        int n = contexto.donacionesRecibidasEnTrimestre(necesidad.entidadId());
        return 1.0 / (1.0 + k * n);
    }
}
