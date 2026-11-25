package ar.utn.donatrack.front.cuenta;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ClienteApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Formulario público de registro: validaciones del BFF y el cuerpo que viaja a Donaciones. */
@SpringBootTest
@AutoConfigureMockMvc
class FormularioRegistroTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    Apis apis;

    private final ClienteApi donaciones = mock(ClienteApi.class);

    @BeforeEach
    void preparar() {
        when(apis.donaciones()).thenReturn(donaciones);
        when(donaciones.post(anyString(), any())).thenReturn(Map.of());
    }

    private static MockHttpServletRequestBuilder registroEntidad() {
        return post("/registro/entidad").with(csrf()).with(r -> { r.setRemoteAddr("198.51.100.30"); return r; })
                .param("razonSocial", "Escuela 12").param("calle", "Medrano").param("altura", "951")
                .param("telefono", "1144445555").param("emails", "escuela@x.org; direccion@x.org");
    }

    @Test
    void conClavesDistintasVuelveAlFormularioSinLlamarAlServicio() throws Exception {
        mvc.perform(registroEntidad().param("clave", "Segura123!").param("confirmacion", "Otra123!").param("terminos", "on"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Las contraseñas no coinciden")))
                .andExpect(content().string(containsString("Escuela 12"))); // no pierde lo cargado
        verify(donaciones, never()).post(anyString(), any());
    }

    @Test
    void sinAceptarTerminosNoSeRegistra() throws Exception {
        mvc.perform(registroEntidad().param("clave", "Segura123!").param("confirmacion", "Segura123!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aceptar los términos")));
        verify(donaciones, never()).post(anyString(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unRegistroValidoLlegaADonacionesYMandaAlLogin() throws Exception {
        mvc.perform(registroEntidad().param("clave", "Segura123!").param("confirmacion", "Segura123!").param("terminos", "on"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));

        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(donaciones).post(org.mockito.ArgumentMatchers.eq("/api/registro/entidades"), cuerpo.capture());
        Map<String, Object> enviado = (Map<String, Object>) cuerpo.getValue();
        assertThat(enviado).containsEntry("razonSocial", "Escuela 12").containsEntry("clave", "Segura123!");
        assertThat(enviado.get("emails")).isEqualTo(List.of("escuela@x.org", "direccion@x.org"));
    }

    private static MockHttpServletRequestBuilder registroDonanteHumano(String ip) {
        return post("/registro/donante-humano").with(csrf()).with(r -> { r.setRemoteAddr(ip); return r; })
                .param("nombre", "Lucía").param("apellido", "Gómez").param("edad", "28").param("email", "lucia@x.org")
                .param("clave", "Segura123!").param("confirmacion", "Segura123!").param("terminos", "on");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cuerpoEnviadoAlRegistroHumano() {
        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        verify(donaciones).post(org.mockito.ArgumentMatchers.eq("/api/registro/donantes/humanas"), cuerpo.capture());
        return (Map<String, Object>) cuerpo.getValue();
    }

    @Test
    void laDifusionDeLogrosEsOptIn() throws Exception {
        mvc.perform(registroDonanteHumano("198.51.100.32")).andExpect(status().isFound());

        assertThat(cuerpoEnviadoAlRegistroHumano()).containsEntry("aceptaDifusionLogros", false);
    }

    @Test
    void marcandoLaCasillaSeAceptaLaDifusion() throws Exception {
        mvc.perform(registroDonanteHumano("198.51.100.33").param("difusionLogros", "on")).andExpect(status().isFound());

        assertThat(cuerpoEnviadoAlRegistroHumano()).containsEntry("aceptaDifusionLogros", true);
    }

    @Test
    void elFormularioOfreceLaCasillaDesmarcada() throws Exception {
        String html = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/registro/donante-humano"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).containsPattern("<input[^>]*name=\"difusionLogros\"")
                .doesNotContainPattern("<input[^>]*name=\"difusionLogros\"[^>]*checked")
                .doesNotContainPattern("<input[^>]*checked[^>]*name=\"difusionLogros\"");
    }

    @Test
    void sinTokenCsrfElRegistroSeRechaza() throws Exception {
        mvc.perform(post("/registro/entidad").with(r -> { r.setRemoteAddr("198.51.100.31"); return r; })
                        .param("razonSocial", "Escuela 12").param("clave", "x").param("confirmacion", "x").param("terminos", "on"))
                .andExpect(status().isForbidden());
        verify(donaciones, never()).post(anyString(), any());
    }
}
