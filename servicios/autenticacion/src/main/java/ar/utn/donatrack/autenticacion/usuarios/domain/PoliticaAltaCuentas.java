package ar.utn.donatrack.autenticacion.usuarios.domain;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Quién puede crear qué cuentas (menor privilegio). Los servicios sólo dan de alta donantes y
 * entidades (Donaciones: registro e importación). Roles con poder (ADMIN, CHOFER) sólo los crea
 * una persona administradora: si se filtra el secreto de un servicio, no se escala a administrador.
 */
public final class PoliticaAltaCuentas {

    static final Set<Rol> CREABLES_POR_SERVICIO = EnumSet.of(Rol.DONANTE, Rol.BENEFICIARIA);

    private PoliticaAltaCuentas() {
    }

    /** @param autoridades del solicitante, en formato Spring ("ROLE_ADMIN", "ROLE_SERVICIO") */
    public static void exigir(Collection<String> autoridades, Rol nuevo) {
        if (autoridades.contains("ROLE_ADMIN")) {
            return;
        }
        if (autoridades.contains("ROLE_SERVICIO") && CREABLES_POR_SERVICIO.contains(nuevo)) {
            return;
        }
        throw new OperacionNoPermitidaException("No tenés permiso para crear cuentas con rol " + nuevo);
    }
}
