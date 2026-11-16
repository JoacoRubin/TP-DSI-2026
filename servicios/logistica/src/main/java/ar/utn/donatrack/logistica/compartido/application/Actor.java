package ar.utn.donatrack.logistica.compartido.application;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;

import java.util.UUID;

/**
 * Quién ejecuta la operación. {@code referencia} es la patente para choferes y el id de la
 * entidad/donante para el resto.
 */
public record Actor(String usuario, String rol, String referencia) {

    public static final Actor SISTEMA = new Actor("sistema", "SERVICIO", null);

    public boolean esAdmin() {
        return "ADMIN".equals(rol) || "SERVICIO".equals(rol);
    }

    public void exigirChoferDe(String patente) {
        if (!esAdmin() && !("CHOFER".equals(rol) && patente.equalsIgnoreCase(referencia))) {
            throw new OperacionNoPermitidaException("Sólo el chofer del camión " + patente + " puede operar esta ruta");
        }
    }

    public void exigirEntidad(UUID entidadId) {
        if (!esAdmin() && !("BENEFICIARIA".equals(rol) && entidadId.toString().equals(referencia))) {
            throw new OperacionNoPermitidaException("Sólo la entidad destinataria puede operar esta entrega");
        }
    }
}
