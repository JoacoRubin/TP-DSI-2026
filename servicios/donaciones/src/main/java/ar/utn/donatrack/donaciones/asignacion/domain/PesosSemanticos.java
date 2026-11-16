package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

/** Constantes w1, w2, w3 configurables por la administración. Regla de oro: suman 1. */
public record PesosSemanticos(double textual, double volumen, double tipo) {

    public static final PesosSemanticos PREDETERMINADOS = new PesosSemanticos(0.4, 0.4, 0.2);

    public PesosSemanticos {
        for (double w : new double[]{textual, volumen, tipo}) {
            if (w < 0 || w > 1) {
                throw new ReglaDeNegocioException("Cada peso debe estar entre 0 y 1");
            }
        }
        if (Math.abs(textual + volumen + tipo - 1.0) > 1e-6) {
            throw new ReglaDeNegocioException("La suma de los pesos debe ser exactamente 1 (100%)");
        }
    }
}
