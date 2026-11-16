package ar.utn.donatrack.logistica.rutas.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;

import java.time.Instant;
import java.util.Objects;

/** Reporte de geolocalización de la app del chofer. Se valida antes de procesarse. */
public record Posicion(double latitud, double longitud, double velocidadKmh, Instant registradaEn) {

    public static final double VELOCIDAD_MAXIMA = 200;

    public Posicion {
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            throw new ReglaDeNegocioException("Coordenadas fuera de rango");
        }
        if (velocidadKmh < 0 || velocidadKmh > VELOCIDAD_MAXIMA) {
            throw new ReglaDeNegocioException("Velocidad inválida: " + velocidadKmh);
        }
        Objects.requireNonNull(registradaEn, "registradaEn");
    }
}
