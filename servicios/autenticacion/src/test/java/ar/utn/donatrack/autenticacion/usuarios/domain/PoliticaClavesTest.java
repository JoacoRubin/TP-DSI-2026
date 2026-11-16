package ar.utn.donatrack.autenticacion.usuarios.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaClavesTest {

    @ParameterizedTest
    @ValueSource(strings = {"corta1", "sinnumeros", "12345678", ""})
    void rechazaClavesDebiles(String clave) {
        assertThatThrownBy(() -> PoliticaClaves.validar(clave)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void aceptaUnaClaveConLetrasYNumerosDe8OMas() {
        assertThatCode(() -> PoliticaClaves.validar("Donar2026")).doesNotThrowAnyException();
    }
}
