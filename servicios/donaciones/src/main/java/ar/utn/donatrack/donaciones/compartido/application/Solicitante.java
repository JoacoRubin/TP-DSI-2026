package ar.utn.donatrack.donaciones.compartido.application;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;

import java.util.UUID;

/** Quién ejecuta el caso de uso (para autorización a nivel de recurso y auditoría). */
public record Solicitante(String usuario, String rol, UUID referencia) {

    public static final Solicitante SISTEMA = new Solicitante("sistema", "SERVICIO", null);

    public boolean esAdmin() {
        return "ADMIN".equals(rol) || "SERVICIO".equals(rol);
    }

    public boolean esDonante(UUID donanteId) {
        return "DONANTE".equals(rol) && donanteId.equals(referencia);
    }

    public boolean esEntidad(UUID entidadId) {
        return "BENEFICIARIA".equals(rol) && entidadId != null && entidadId.equals(referencia);
    }

    public void exigirAdminOEntidad(UUID entidadId) {
        if (!esAdmin() && !esEntidad(entidadId)) {
            throw new OperacionNoPermitidaException("Sólo la propia entidad o la administración pueden hacer esto");
        }
    }

    public void exigirAdminODonante(UUID donanteId) {
        if (!esAdmin() && !esDonante(donanteId)) {
            throw new OperacionNoPermitidaException("Sólo la propia persona donante o la administración pueden hacer esto");
        }
    }
}
