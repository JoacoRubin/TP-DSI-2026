package ar.utn.donatrack.front.sesion;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ClienteApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz rol × sección del front (BFF). Protege contra la regresión más peligrosa: que un cambio en
 * {@link ConfiguracionSeguridadFront} deje a un rol entrar a la sección de otro.
 * <ul>
 *   <li>Denegado: 403 y se muestra /error/403.</li>
 *   <li>Anónimo en sección privada: redirige al login.</li>
 *   <li>Permitido: la seguridad deja pasar (lo que haga después el controlador no es asunto de este test).</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacionPorRolTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    Apis apis;

    @BeforeEach
    void servicios() {
        ClienteApi cualquiera = mock(ClienteApi.class, RETURNS_DEEP_STUBS);
        when(apis.donaciones()).thenReturn(cualquiera);
        when(apis.logistica()).thenReturn(cualquiera);
        when(apis.incentivos()).thenReturn(cualquiera);
        when(apis.notificaciones()).thenReturn(cualquiera);
        when(apis.autenticacion()).thenReturn(cualquiera);
    }

    enum Quien { ADMIN, DONANTE_HUMANO, DONANTE_JURIDICO, BENEFICIARIA, CHOFER }

    private static UsernamePasswordAuthenticationToken sesion(Quien q) {
        String rol = switch (q) {
            case DONANTE_HUMANO, DONANTE_JURIDICO -> "DONANTE";
            default -> q.name();
        };
        String perfil = switch (q) {
            case DONANTE_HUMANO -> "HUMANA";
            case DONANTE_JURIDICO -> "JURIDICA";
            default -> null;
        };
        UsuarioSesion u = new UsuarioSesion("u-" + q, q.name().toLowerCase() + "@x.org", "Prueba", rol, perfil,
                "33333333-3333-3333-3333-333333333333", "token");
        return new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    /** IP propia para no consumir el cupo del limitador que usan otros tests del mismo contexto. */
    private static final RequestPostProcessor IP = r -> {
        r.setRemoteAddr("198.51.100.20");
        return r;
    };

    // Una URL representativa por sección, con el único rol que puede entrar.
    static Stream<Arguments> secciones() {
        return Stream.of(
                Arguments.of("/admin", Quien.ADMIN),
                Arguments.of("/admin/donaciones", Quien.ADMIN),
                Arguments.of("/admin/camiones", Quien.ADMIN),
                Arguments.of("/front-api/monitoreo", Quien.ADMIN),
                Arguments.of("/donante", Quien.DONANTE_HUMANO),
                Arguments.of("/donante/donaciones", Quien.DONANTE_HUMANO),
                Arguments.of("/entidad", Quien.BENEFICIARIA),
                Arguments.of("/entidad/necesidades", Quien.BENEFICIARIA),
                Arguments.of("/chofer", Quien.CHOFER));
    }

    static Stream<Arguments> otrosRolesEnCadaSeccion() {
        return secciones().flatMap(a -> {
            String url = (String) a.get()[0];
            Quien dueno = (Quien) a.get()[1];
            return Stream.of(Quien.values())
                    .filter(q -> !mismoRol(q, dueno))
                    .map(q -> Arguments.of(url, q));
        });
    }

    private static boolean mismoRol(Quien a, Quien b) {
        return sesion(a).getAuthorities().equals(sesion(b).getAuthorities());
    }

    @ParameterizedTest(name = "{1} NO entra a {0}")
    @MethodSource("otrosRolesEnCadaSeccion")
    void unRolNoEntraALaSeccionDeOtro(String url, Quien quien) throws Exception {
        mvc.perform(get(url).with(authentication(sesion(quien))).with(IP))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/error/403"));
    }

    @ParameterizedTest(name = "{1} entra a {0}")
    @MethodSource("secciones")
    void cadaRolEntraASuSeccion(String url, Quien quien) throws Exception {
        assertThat(pasaLaSeguridad(get(url).with(authentication(sesion(quien))).with(IP))).isTrue();
    }

    @ParameterizedTest(name = "anónimo en {0} va al login")
    @MethodSource("secciones")
    void sinSesionSeVaAlLogin(String url, Quien ignorado) throws Exception {
        mvc.perform(get(url).with(IP))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest(name = "{0} es público")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/login", "/registro", "/registro/entidad", "/legal", "/accesibilidad"})
    void lasPaginasPublicasNoPidenSesion(String url) throws Exception {
        mvc.perform(get(url).with(IP)).andExpect(status().isOk());
    }

    @org.junit.jupiter.api.Test
    void losIncentivosSonSoloParaDonantesHumanos() throws Exception {
        mvc.perform(get("/donante/incentivos").with(authentication(sesion(Quien.DONANTE_JURIDICO))).with(IP))
                .andExpect(status().isForbidden());
        assertThat(pasaLaSeguridad(get("/donante/incentivos").with(authentication(sesion(Quien.DONANTE_HUMANO))).with(IP)))
                .isTrue();
    }

    @org.junit.jupiter.api.Test
    void elReporteGpsEsSoloDelChofer() throws Exception {
        String url = "/front-api/chofer/rutas/44444444-4444-4444-4444-444444444444/posiciones";
        mvc.perform(post(url).with(csrf()).with(authentication(sesion(Quien.ADMIN))).with(IP)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    void unFormularioSinTokenCsrfSeRechazaAunqueElRolSeaCorrecto() throws Exception {
        mvc.perform(post("/entidad/necesidades").with(authentication(sesion(Quien.BENEFICIARIA))).with(IP)
                        .param("descripcion", "Sillas"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/camiones").with(authentication(sesion(Quien.ADMIN))).with(IP)
                        .param("patente", "AE123BC"))
                .andExpect(status().isForbidden());
    }

    /**
     * La seguridad dejó pasar si la respuesta no es una denegación ni un rebote al login. Si el
     * controlador explota con los servicios simulados, la excepción viene DESPUÉS de la seguridad.
     */
    private boolean pasaLaSeguridad(MockHttpServletRequestBuilder pedido) {
        try {
            MockHttpServletResponse r = mvc.perform(pedido).andReturn().getResponse();
            boolean denegado = r.getStatus() == 401 || r.getStatus() == 403;
            boolean alLogin = r.getRedirectedUrl() != null && r.getRedirectedUrl().endsWith("/login");
            return !denegado && !alLogin;
        } catch (Exception e) {
            Throwable causa = e;
            while (causa != null) {
                if (causa instanceof AccessDeniedException) {
                    return false;
                }
                causa = causa.getCause();
            }
            return true;
        }
    }
}
