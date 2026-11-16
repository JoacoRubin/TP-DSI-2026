package ar.utn.donatrack.incentivos.misiones.domain;

import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;

import java.time.YearMonth;

/**
 * Strategy implementado con un enum: cada tipo de misión sabe medir el progreso del donante.
 * Agregar un tipo = agregar una constante con su criterio.
 */
public enum TipoMision {

    /** Realizar una donación durante X meses consecutivos (puede perder progreso). */
    RACHA {
        @Override
        public int progreso(HistorialDonante h, YearMonth mesActual) {
            return h.rachaMeses(mesActual);
        }
    },
    /** Realizar donaciones de X categorías distintas. */
    COMPLETITUD {
        @Override
        public int progreso(HistorialDonante h, YearMonth mesActual) {
            return h.categoriasDistintas();
        }
    },
    /** Una donación (carga) que supere X cantidad de bienes. */
    HABIL_DONADOR {
        @Override
        public int progreso(HistorialDonante h, YearMonth mesActual) {
            return h.mayorCarga();
        }
    },
    /** Lograr X donaciones recibidas exitosamente por una entidad beneficiaria. */
    DONACIONES_EXITOSAS {
        @Override
        public int progreso(HistorialDonante h, YearMonth mesActual) {
            return h.donacionesExitosas();
        }
    };

    public abstract int progreso(HistorialDonante historial, YearMonth mesActual);
}
