package ar.utn.donatrack.autenticacion.usuarios.domain;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Hallazgo de auditoría: cualquier token SERVICIO podía crear una cuenta ADMIN (escalada de privilegios). */
class PoliticaAltaCuentasTest {

    @Test
    void unServicioSoloCreaCuentasDeDonantesYEntidades() {
        assertThatCode(() -> PoliticaAltaCuentas.exigir(Set.of("ROLE_SERVICIO"), Rol.DONANTE)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaAltaCuentas.exigir(Set.of("ROLE_SERVICIO"), Rol.BENEFICIARIA)).doesNotThrowAnyException();
        assertThatThrownBy(() -> PoliticaAltaCuentas.exigir(Set.of("ROLE_SERVICIO"), Rol.ADMIN))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> PoliticaAltaCuentas.exigir(Set.of("ROLE_SERVICIO"), Rol.CHOFER))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void unaPersonaAdministradoraCreaCualquierRol() {
        for (Rol rol : Rol.values()) {
            assertThatCode(() -> PoliticaAltaCuentas.exigir(Set.of("ROLE_ADMIN"), rol)).doesNotThrowAnyException();
        }
    }

    @Test
    void sinRolAutorizadoNoSeCreaNada() {
        assertThatThrownBy(() -> PoliticaAltaCuentas.exigir(Set.of("ROLE_DONANTE"), Rol.DONANTE))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> PoliticaAltaCuentas.exigir(Set.of(), Rol.DONANTE))
                .isInstanceOf(OperacionNoPermitidaException.class);
    }
}
