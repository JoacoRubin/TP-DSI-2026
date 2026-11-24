package ar.utn.donatrack.donaciones;

import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.catalogo.domain.Categoria;
import ar.utn.donatrack.donaciones.catalogo.domain.Subcategoria;
import ar.utn.donatrack.donaciones.compartido.application.Notificador;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import ar.utn.donatrack.donaciones.compartido.application.ServicioCuentas;
import ar.utn.donatrack.donaciones.compartido.application.ServicioLogistica;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración: contexto Spring real + JPA sobre H2 en memoria + seguridad. Sólo se
 * reemplazan los puertos que salen del proceso (otros servicios y el broker).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@ActiveProfiles("test")
class DonacionesApiIntegracionTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    CatalogoRepositorio catalogo;

    @MockitoBean
    ServicioCuentas cuentas;
    @MockitoBean
    ServicioLogistica logistica;
    @MockitoBean
    Notificador notificador;
    @MockitoBean
    PublicadorEventos eventos;

    private final Subcategoria sillas = new Subcategoria(UUID.randomUUID(), "Sillas");

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.claim("rol", "ADMIN").claim("email", "admin@donatrack.org"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor donante(String id) {
        return jwt().jwt(j -> j.claim("rol", "DONANTE").claim("email", "ana@mail.com").claim("ref", id))
                .authorities(new SimpleGrantedAuthority("ROLE_DONANTE"));
    }

    @BeforeEach
    void preparar() {
        catalogo.guardar(new Categoria(UUID.randomUUID(), "Mobiliario " + UUID.randomUUID(), false, true, List.of(sillas)));
        when(cuentas.crear(any())).thenAnswer(inv -> {
            ServicioCuentas.NuevaCuenta c = inv.getArgument(0);
            return new ServicioCuentas.CuentaCreada(c.email(), true, "token");
        });
    }

    private String registrarDonante(String email) throws Exception {
        String body = """
                {"nombre":"Ana","apellido":"Pérez","edad":34,"tipoDocumento":"DNI","numeroDocumento":"30123456",
                 "genero":"FEMENINO","direccion":{"calle":"Medrano","altura":"951","localidad":"CABA"},
                 "contacto":{"email":"%s"}}""".formatted(email);
        String r = mvc.perform(post("/api/donantes/humanas").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("id").asText();
    }

    @Test
    void saludoPublico() throws Exception {
        mvc.perform(get("/api/saludo"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hola desde el servicio de Donaciones."));
    }

    @Test
    void exponeMetricasParaPrometheusEtiquetadasConElServicio() throws Exception {
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("application=\"donaciones\"")));
    }

    @Test
    void sinTokenNoSePuedeRegistrarDonaciones() throws Exception {
        mvc.perform(post("/api/donaciones").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unDonanteNoPuedeRegistrarDonaciones() throws Exception {
        mvc.perform(post("/api/donaciones").with(donante(UUID.randomUUID().toString()))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void flujoCompleto_registrarCargaSegmentarConsultarYVencer() throws Exception {
        String donanteId = registrarDonante("ana" + UUID.randomUUID() + "@mail.com");

        String carga = """
                {"donanteId":"%s","descripcionGeneral":"Mudanza",
                 "bienes":[{"descripcion":"Silla","subcategoriaId":"%s","cantidad":6,"estadoUso":"USADO"},
                           {"descripcion":"Silla plegable","subcategoriaId":"%s","cantidad":4,"estadoUso":"USADO"}]}"""
                .formatted(donanteId, sillas.id(), sillas.id());
        String r = mvc.perform(post("/api/donaciones").with(admin()).contentType(MediaType.APPLICATION_JSON).content(carga))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cantidadTotal").value(10))
                .andExpect(jsonPath("$[0].estado").value("EN_DEPOSITO"))
                .andReturn().getResponse().getContentAsString();
        String donacionId = json.readTree(r).get(0).get("id").asText();

        // El donante ve su donación (y sólo las suyas, aunque pida otras)
        mvc.perform(get("/api/donaciones").param("donanteId", UUID.randomUUID().toString()).with(donante(donanteId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mvc.perform(patch("/api/donaciones/" + donacionId + "/estado").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"VENCIDA\",\"motivo\":\"Humedad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VENCIDA"))
                .andExpect(jsonPath("$.historial.length()").value(2));
    }

    @Test
    void lasReglasDeNegocioSeDevuelvenComo422ConDetalle() throws Exception {
        String donanteId = registrarDonante("beto" + UUID.randomUUID() + "@mail.com");
        String carga = """
                {"donanteId":"%s","bienes":[{"descripcion":"Silla","subcategoriaId":"%s","cantidad":1}]}"""
                .formatted(donanteId, sillas.id());
        String r = mvc.perform(post("/api/donaciones").with(admin()).contentType(MediaType.APPLICATION_JSON).content(carga))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();
        JsonNode problema = json.readTree(r);
        assertThat(problema.get("detail").asText()).contains("nuevo o usado");
    }

    @Test
    void lasValidacionesDeEntradaDevuelven400ConLosCampos() throws Exception {
        mvc.perform(post("/api/donantes/humanas").with(admin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }
}
