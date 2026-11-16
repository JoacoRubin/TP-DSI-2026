package ar.utn.donatrack.donaciones.asignacion.domain;

import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;

/**
 * Strategy de puntuación. Cada algoritmo devuelve un score normalizado en [0; 1] para un
 * cruce (donación, necesidad). Para sumar un criterio nuevo se agrega una implementación:
 * el motor y los casos de uso no cambian.
 */
public interface AlgoritmoMatchmaking {

    String nombre();

    double puntuar(Donacion donacion, Necesidad necesidad, ContextoMatchmaking contexto);
}
