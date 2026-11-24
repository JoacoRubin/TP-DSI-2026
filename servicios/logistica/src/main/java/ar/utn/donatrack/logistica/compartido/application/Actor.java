package ar.utn.donatrack.logistica.compartido.application;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.logistica.rutas.domain.Ruta;

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

    /**
     * Ver una ruta expone patente, posición y recorrido: sólo admin, el chofer del camión o un
     * donante/entidad con alguna entrega en esa ruta.
     */
    public void exigirAccesoA(Ruta ruta) {
        if ("CHOFER".equals(rol)) {
            exigirChoferDe(ruta.patente());
            return;
        }
        if (esAdmin()) {
            return;
        }
        UUID ref = referenciaComoUuid();
        boolean involucrado = ref != null && switch (rol == null ? "" : rol) {
            case "DONANTE" -> ruta.involucraDonante(ref);
            case "BENEFICIARIA" -> ruta.involucraEntidad(ref);
            default -> false;
        };
        if (!involucrado) {
            throw new OperacionNoPermitidaException("No tenés entregas en esta ruta");
        }
    }

    private UUID referenciaComoUuid() {
        try {
            return referencia == null ? null : UUID.fromString(referencia);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void exigirEntidad(UUID entidadId) {
        if (!esAdmin() && !("BENEFICIARIA".equals(rol) && entidadId.toString().equals(referencia))) {
            throw new OperacionNoPermitidaException("Sólo la entidad destinataria puede operar esta entrega");
        }
    }
}
