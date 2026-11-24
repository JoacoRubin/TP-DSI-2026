package ar.utn.donatrack.front.limites;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El filtro está registrado en la app real y corre ANTES de Spring Security: sin token CSRF las
 * primeras solicitudes dan 403, pero al agotar el cupo la respuesta es 429 sin llegar a la seguridad.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LimiteSolicitudesIntegracionTest {

    @Autowired
    MockMvc mvc;

    @Test
    void elLoginRepetidoDesdeLaMismaIpTermina429() throws Exception {
        for (int i = 0; i < PoliticaLimite.AUTENTICACION.capacidad(); i++) {
            mvc.perform(post("/login").with(r -> { r.setRemoteAddr("203.0.113.7"); return r; })
                    .param("email", "x@x.com").param("clave", "mala"));
        }
        mvc.perform(post("/login").with(r -> { r.setRemoteAddr("203.0.113.7"); return r; })
                        .param("email", "x@x.com").param("clave", "mala"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
