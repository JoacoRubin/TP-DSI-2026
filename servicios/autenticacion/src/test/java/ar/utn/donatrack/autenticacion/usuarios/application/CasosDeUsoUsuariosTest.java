package ar.utn.donatrack.autenticacion.usuarios.application;

import ar.utn.donatrack.autenticacion.usuarios.domain.EstadoUsuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.Rol;
import ar.utn.donatrack.autenticacion.usuarios.domain.Usuario;
import ar.utn.donatrack.autenticacion.usuarios.domain.UsuarioRepositorio;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CasosDeUsoUsuariosTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC);
    private RepositorioEnMemoria repositorio;
    private final HasheadorClaves hasheador = new HasheadorClaves() {
        public String hashear(String clave) { return "H(" + clave + ")"; }
        public boolean coincide(String clave, String hash) { return hash.equals("H(" + clave + ")"); }
    };
    private final AtomicInteger secuencia = new AtomicInteger();
    private final GeneradorSecretos secretos = () -> "secreto-" + secuencia.incrementAndGet();
    private final EmisorTokens emisor = new EmisorTokens() {
        public TokenEmitido paraUsuario(Usuario u) { return new TokenEmitido("jwt-" + u.email(), 3600); }
        public TokenEmitido paraServicio(String clientId) { return new TokenEmitido("jwt-" + clientId, 600); }
    };

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioEnMemoria();
    }

    private NuevoUsuario nuevo(String email, String clave) {
        return new NuevoUsuario(email, "Nombre", Rol.DONANTE, "HUMANA", UUID.randomUUID().toString(), clave);
    }

    @Test
    void registrarConClaveCreaUnUsuarioActivoQuePuedeIniciarSesion() {
        new RegistrarUsuario(repositorio, hasheador, secretos, reloj).ejecutar(nuevo("ana@mail.com", "Donar2026"));

        TokenEmitido token = new IniciarSesion(repositorio, hasheador, emisor).ejecutar("ana@mail.com", "Donar2026").token();

        assertThat(token.accessToken()).isEqualTo("jwt-ana@mail.com");
    }

    @Test
    void registrarSinClaveDevuelveTokenDeActivacionYLaCuentaQuedaPendiente() {
        ResultadoAlta alta = new RegistrarUsuario(repositorio, hasheador, secretos, reloj).ejecutar(nuevo("ana@mail.com", null));

        assertThat(alta.tokenActivacion()).isEqualTo("secreto-1");
        assertThat(repositorio.buscarPorEmail("ana@mail.com")).get()
                .extracting(Usuario::estado).isEqualTo(EstadoUsuario.PENDIENTE_ACTIVACION);
    }

    @Test
    void activarCuentaPermiteIngresarConLaNuevaClave() {
        ResultadoAlta alta = new RegistrarUsuario(repositorio, hasheador, secretos, reloj).ejecutar(nuevo("ana@mail.com", null));

        new ActivarCuenta(repositorio, hasheador, reloj).ejecutar(alta.tokenActivacion(), "Nueva2026");

        assertThat(new IniciarSesion(repositorio, hasheador, emisor).ejecutar("ana@mail.com", "Nueva2026")).isNotNull();
    }

    @Test
    void credencialesInvalidasNoRevelanSiElEmailExiste() {
        new RegistrarUsuario(repositorio, hasheador, secretos, reloj).ejecutar(nuevo("ana@mail.com", "Donar2026"));
        IniciarSesion login = new IniciarSesion(repositorio, hasheador, emisor);

        assertThatThrownBy(() -> login.ejecutar("ana@mail.com", "mala1234")).isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
        assertThatThrownBy(() -> login.ejecutar("nadie@mail.com", "mala1234")).isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    void noSePermitenEmailsDuplicados() {
        RegistrarUsuario registrar = new RegistrarUsuario(repositorio, hasheador, secretos, reloj);
        registrar.ejecutar(nuevo("ana@mail.com", "Donar2026"));
        assertThatThrownBy(() -> registrar.ejecutar(nuevo("ANA@mail.com", "Donar2026")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void elLoteCreaLosNuevosYActualizaLosExistentesSinRegenerarCredenciales() {
        new RegistrarUsuario(repositorio, hasheador, secretos, reloj).ejecutar(nuevo("ana@mail.com", "Donar2026"));

        List<ResultadoAlta> resultados = new RegistrarUsuariosEnLote(repositorio, secretos, reloj).ejecutar(List.of(
                new NuevoUsuario("ana@mail.com", "Ana Actualizada", Rol.DONANTE, "HUMANA", "r1", null),
                new NuevoUsuario("beto@mail.com", "Beto", Rol.DONANTE, "HUMANA", "r2", null)));

        assertThat(resultados).extracting(ResultadoAlta::email, ResultadoAlta::creado)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("ana@mail.com", false),
                        org.assertj.core.groups.Tuple.tuple("beto@mail.com", true));
        assertThat(resultados.get(0).tokenActivacion()).isNull();
        assertThat(resultados.get(1).tokenActivacion()).isNotBlank();
        assertThat(repositorio.buscarPorEmail("ana@mail.com")).get().extracting(Usuario::nombre).isEqualTo("Ana Actualizada");
    }

    static class RepositorioEnMemoria implements UsuarioRepositorio {
        private final Map<String, Usuario> porEmail = new HashMap<>();

        public Optional<Usuario> buscarPorEmail(String email) {
            return Optional.ofNullable(porEmail.get(email.trim().toLowerCase()));
        }

        public Optional<Usuario> buscarPorHashActivacion(String hash) {
            return porEmail.values().stream().filter(u -> hash.equals(u.tokenActivacionHash())).findFirst();
        }

        public List<Usuario> buscarPorEmails(Collection<String> emails) {
            return emails.stream().map(porEmail::get).filter(java.util.Objects::nonNull).toList();
        }

        public void guardar(Usuario usuario) {
            porEmail.put(usuario.email(), usuario);
        }

        public void guardarTodos(Collection<Usuario> usuarios) {
            usuarios.forEach(this::guardar);
        }

        public boolean existeEmail(String email) {
            return porEmail.containsKey(email);
        }
    }
}
