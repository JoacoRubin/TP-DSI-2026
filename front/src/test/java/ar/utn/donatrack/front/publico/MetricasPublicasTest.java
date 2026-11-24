package ar.utn.donatrack.front.publico;

import ar.utn.donatrack.front.api.Apis;
import ar.utn.donatrack.front.api.ClienteApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/** Regresión: las cifras públicas no pueden salir de una muestra truncada. */
@SpringBootTest
@AutoConfigureMockMvc
class MetricasPublicasTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    Apis apis;

    private final ClienteApi logistica = mock(ClienteApi.class);

    private static List<Map<String, Object>> entregas(int n) {
        return IntStream.range(0, n).mapToObj(i -> Map.<String, Object>of("id", "e" + i, "descripcion", "Sillas",
                "entidadNombre", "Escuela " + i, "latitud", -34.6, "longitud", -58.4,
                "entregadaEn", "2026-11-10T12:00:00Z", "fotos", List.of())).toList();
    }

    @BeforeEach
    void preparar() {
        when(apis.logistica()).thenReturn(logistica);
        ClienteApi incentivos = mock(ClienteApi.class);
        when(incentivos.get(anyString())).thenReturn(Map.of("entradas", List.of()));
        when(apis.incentivos()).thenReturn(incentivos);
        when(logistica.lista(anyString(), any(), any())).thenReturn(entregas(6));
    }

    @Test
    void laPortadaMuestraElTotalRealDelMesNoElTamanioDeLaMuestra() throws Exception {
        when(logistica.get("/api/publico/resumen?dias={d}", 30)).thenReturn(Map.of("entregadas", 1234, "entidades", 87));

        mvc.perform(get("/"))
                .andExpect(content().string(containsString(">1234<")))
                .andExpect(content().string(containsString(">87<")));
    }

    @Test
    void elRankingDiceMisionEnSingularCuandoEsUna() throws Exception {
        ClienteApi incentivos = mock(ClienteApi.class);
        when(incentivos.get(anyString())).thenReturn(Map.of("id", "2026-11", "entradas", List.of(
                Map.of("nombre", "Ana Pérez", "misionesCumplidas", 1),
                Map.of("nombre", "Arcos", "misionesCumplidas", 3))));
        when(apis.incentivos()).thenReturn(incentivos);

        mvc.perform(get("/"))
                .andExpect(content().string(containsString("1 misión cumplida<")))
                .andExpect(content().string(containsString("3 misiones cumplidas<")));
    }

    @Test
    void elMapaAclaraCuandoMuestraSoloLasMasRecientes() throws Exception {
        when(logistica.lista(anyString(), eq(3650), eq(500))).thenReturn(entregas(500));
        when(logistica.get("/api/publico/resumen?dias={d}", 3650)).thenReturn(Map.of("entregadas", 1234, "entidades", 87));

        mvc.perform(get("/mapa"))
                .andExpect(content().string(containsString("Mostrando las 500 más recientes de 1234 entregas")));
    }

    @Test
    void elMapaDiceTotalSoloCuandoEsElTotal() throws Exception {
        when(logistica.lista(anyString(), eq(3650), eq(500))).thenReturn(entregas(40));
        when(logistica.get("/api/publico/resumen?dias={d}", 3650)).thenReturn(Map.of("entregadas", 40, "entidades", 12));

        mvc.perform(get("/mapa")).andExpect(content().string(containsString("40 entregas en total")));
    }
}
