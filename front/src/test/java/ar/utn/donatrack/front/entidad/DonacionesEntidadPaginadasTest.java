package ar.utn.donatrack.front.entidad;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ClienteApi;
import ar.utn.donatrack.front.sesion.UsuarioSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regresión: la entidad con más de 100 donaciones no podía ver (ni confirmar) las siguientes. */
@SpringBootTest
@AutoConfigureMockMvc
class DonacionesEntidadPaginadasTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    Apis apis;

    private final ClienteApi donaciones = mock(ClienteApi.class);
    private final ClienteApi logistica = mock(ClienteApi.class);

    private static final String D1 = "11111111-1111-1111-1111-111111111111";
    private static final String D2 = "22222222-2222-2222-2222-222222222222";

    private static Map<String, Object> donacion(String id) {
        return Map.of("id", id, "descripcionCorta", "Sillas " + id.charAt(0), "categoria", "Mobiliario",
                "registradaEn", "2026-11-10T12:00:00Z", "estado", "ASIGNADA", "estadoEtiqueta", "Asignación realizada");
    }

    @BeforeEach
    void preparar() {
        when(apis.donaciones()).thenReturn(donaciones);
        when(apis.logistica()).thenReturn(logistica);
        when(apis.notificaciones()).thenReturn(mock(ClienteApi.class));
        when(donaciones.get(anyString())).thenReturn(Map.of("elementos", List.of(donacion(D1), donacion(D2)),
                "pagina", 1, "tamanio", 20, "total", 42, "totalPaginas", 3));
        when(logistica.lista(anyString())).thenReturn(List.of());
    }

    private static UsernamePasswordAuthenticationToken escuela() {
        UsuarioSesion u = new UsuarioSesion("u1", "escuela@x.org", "Escuela", "BENEFICIARIA", null,
                "33333333-3333-3333-3333-333333333333", "token");
        return new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_BENEFICIARIA")));
    }

    @Test
    void pideLaPaginaElegidaYSoloLasEntregasDeEsaPagina() throws Exception {
        mvc.perform(get("/entidad/donaciones").param("pagina", "1").with(authentication(escuela())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Página 2 de 3")));

        verify(donaciones).get("/api/donaciones?pagina=1&tamanio=20");
        verify(logistica).lista("/api/entregas?donacionIds=" + D1 + "," + D2);
    }
}
