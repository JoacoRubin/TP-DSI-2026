package ar.utn.donatrack.logistica.compartido.infrastructure.web;

import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.entregas.domain.FiltroEntregas;
import ar.utn.donatrack.logistica.entregas.domain.Pagina;
import ar.utn.donatrack.logistica.rutas.application.ConsultasLogistica;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@GraphQlTest(EntregasGraphQl.class)
class EntregasGraphQlTest {

    @Autowired
    GraphQlTester graphQl;
    @MockitoBean
    ConsultasLogistica consultas;

    private final UUID escuela = UUID.randomUUID();

    private Entrega entrega() {
        return Entrega.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Ana", escuela,
                new Destino("Escuela 12", "Ruta 6 km 32", -35.05, -58.76),
                new DetalleDonacion("6 unidades de Sillas", "Sillas", 6, "unidades"), 30, 1.2,
                Instant.parse("2026-11-10T12:00:00Z"));
    }

    @Test
    void filtraDinamicamenteYDevuelveSoloLosCamposPedidos() {
        when(consultas.buscarEntregas(any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(entrega()), 41, 2, 20));

        Map<String, Object> respuesta = graphQl.document("""
                        query($entidad: ID) {
                          entregas(filtro: { estados: [PENDIENTE, PLANIFICADA], entidadId: $entidad, texto: "sillas",
                                             desde: "2026-11-01T00:00:00Z" }, pagina: 2) {
                            total
                            elementos { entidadNombre descripcion estado }
                          }
                        }""")
                .variable("entidad", escuela.toString())
                .execute()
                .path("entregas").entity(Map.class).get();

        assertThat(respuesta).containsEntry("total", 41);
        assertThat((List<?>) respuesta.get("elementos")).singleElement()
                .isEqualTo(Map.of("entidadNombre", "Escuela 12", "descripcion", "6 unidades de Sillas", "estado", "PENDIENTE"));

        ArgumentCaptor<FiltroEntregas> filtro = ArgumentCaptor.forClass(FiltroEntregas.class);
        verify(consultas).buscarEntregas(filtro.capture(), eq(2), eq(20));
        assertThat(filtro.getValue().estados()).containsExactlyInAnyOrder(EstadoEntrega.PENDIENTE, EstadoEntrega.PLANIFICADA);
        assertThat(filtro.getValue().entidadId()).isEqualTo(escuela);
        assertThat(filtro.getValue().texto()).isEqualTo("sillas");
        assertThat(filtro.getValue().desde()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void sinFiltroUsaLosValoresPorDefecto() {
        when(consultas.buscarEntregas(any(), anyInt(), anyInt())).thenReturn(new Pagina<>(List.of(), 0, 0, 20));

        graphQl.document("{ entregas { total } }").execute().path("entregas.total").entity(Integer.class).isEqualTo(0);

        verify(consultas).buscarEntregas(eq(FiltroEntregas.sinFiltros()), eq(0), eq(20));
    }
}
