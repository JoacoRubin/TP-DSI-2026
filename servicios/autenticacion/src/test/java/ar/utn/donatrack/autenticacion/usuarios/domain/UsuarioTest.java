package ar.utn.donatrack.autenticacion.usuarios.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private final Instant ahora = Instant.parse("2026-11-16T12:00:00Z");

    private Usuario pendiente() {
        return Usuario.pendienteDeActivacion(UUID.randomUUID(), "ana@mail.com", "Ana", Rol.DONANTE, "HUMANA",
                "ref", "hash-token", ahora.plus(Duration.ofDays(7)), ahora);
    }

    @Test
    void unUsuarioPendienteNoPuedeIniciarSesion() {
        assertThatThrownBy(() -> pendiente().verificarHabilitadoParaIngresar())
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("activar");
    }

    @Test
    void activarConTokenCorrectoDejaAlUsuarioActivoConClave() {
        Usuario u = pendiente();
        u.activar("hash-token", "hash-clave", ahora.plusSeconds(60));
        assertThat(u.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(u.claveHash()).isEqualTo("hash-clave");
        u.verificarHabilitadoParaIngresar();
    }

    @Test
    void elTokenDeActivacionEsDeUnSoloUso() {
        Usuario u = pendiente();
        u.activar("hash-token", "hash-clave", ahora);
        assertThatThrownBy(() -> u.activar("hash-token", "otra", ahora))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void noSePuedeActivarConTokenVencido() {
        Usuario u = pendiente();
        assertThatThrownBy(() -> u.activar("hash-token", "hash-clave", ahora.plus(Duration.ofDays(8))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("venció");
    }

    @Test
    void noSePuedeActivarConTokenIncorrecto() {
        assertThatThrownBy(() -> pendiente().activar("otro-hash", "hash-clave", ahora))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void elEmailSeNormalizaEnMinusculas() {
        Usuario u = Usuario.conClave(UUID.randomUUID(), "  Ana@Mail.COM ", "Ana", Rol.DONANTE, null, null, "h", ahora);
        assertThat(u.email()).isEqualTo("ana@mail.com");
    }

    @Test
    void rechazaEmailsInvalidos() {
        assertThatThrownBy(() -> Usuario.conClave(UUID.randomUUID(), "no-es-email", "Ana", Rol.DONANTE, null, null, "h", ahora))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
