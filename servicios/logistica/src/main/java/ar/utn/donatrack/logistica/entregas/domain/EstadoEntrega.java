package ar.utn.donatrack.logistica.entregas.domain;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoEntrega {
    PENDIENTE("Pendiente"),
    EN_PLANIFICACION("En planificación"),
    PLANIFICADA("Planificada"),
    EN_TRASLADO("En traslado"),
    ENTREGADA("Entregada"),
    NO_RECIBIDA("No recibida"),
    CANCELADA("Cancelada");

    private final String etiqueta;

    EstadoEntrega(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public Set<EstadoEntrega> siguientes() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(EN_PLANIFICACION, CANCELADA);
            case EN_PLANIFICACION -> EnumSet.of(PLANIFICADA, PENDIENTE, CANCELADA);
            case PLANIFICADA -> EnumSet.of(EN_TRASLADO, PENDIENTE, CANCELADA);
            case EN_TRASLADO -> EnumSet.of(ENTREGADA, NO_RECIBIDA);
            case NO_RECIBIDA -> EnumSet.of(PENDIENTE, CANCELADA);
            case ENTREGADA, CANCELADA -> EnumSet.noneOf(EstadoEntrega.class);
        };
    }

    public boolean esFinal() {
        return this == ENTREGADA || this == NO_RECIBIDA || this == CANCELADA;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
