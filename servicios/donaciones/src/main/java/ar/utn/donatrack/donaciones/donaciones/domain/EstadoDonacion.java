package ar.utn.donatrack.donaciones.donaciones.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados de la donación. Las transiciones válidas están declaradas en UN solo
 * lugar: cualquier cambio de estado que no figure acá es imposible por construcción.
 */
public enum EstadoDonacion {
    EN_DEPOSITO("En depósito"),
    ASIGNADA("Asignación realizada"),
    LISTA_PARA_ENTREGAR("Lista para entregar"),
    EN_TRASLADO("En traslado"),
    ENTREGADA("Entregada"),
    ENTREGA_FALLIDA("Entrega fallida"),
    VENCIDA("Vencida");

    private final String etiqueta;

    EstadoDonacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public Set<EstadoDonacion> siguientes() {
        return switch (this) {
            case EN_DEPOSITO -> EnumSet.of(ASIGNADA, VENCIDA);
            case ASIGNADA -> EnumSet.of(LISTA_PARA_ENTREGAR, EN_DEPOSITO, VENCIDA);
            case LISTA_PARA_ENTREGAR -> EnumSet.of(EN_TRASLADO, VENCIDA);
            case EN_TRASLADO -> EnumSet.of(ENTREGADA, ENTREGA_FALLIDA);
            case ENTREGA_FALLIDA -> EnumSet.of(LISTA_PARA_ENTREGAR, EN_DEPOSITO, VENCIDA);
            case ENTREGADA, VENCIDA -> EnumSet.noneOf(EstadoDonacion.class);
        };
    }

    public boolean puedePasarA(EstadoDonacion destino) {
        return siguientes().contains(destino);
    }

    /** Mientras está en alguno de estos estados, la donación está físicamente en el depósito. */
    public boolean estaEnDeposito() {
        return this == EN_DEPOSITO || this == ASIGNADA || this == LISTA_PARA_ENTREGAR || this == ENTREGA_FALLIDA;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
