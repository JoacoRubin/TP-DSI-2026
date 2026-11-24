package ar.utn.donatrack.logistica.compartido.infrastructure.web;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.logistica.compartido.infrastructure.web.Vistas.EntregaVista;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.entregas.domain.FiltroEntregas;
import ar.utn.donatrack.logistica.entregas.domain.Pagina;
import ar.utn.donatrack.logistica.rutas.application.ConsultasLogistica;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador GraphQL: traduce el input del esquema (graphql/logistica.graphqls) al filtro de
 * dominio. La selección de campos la resuelve GraphQL sobre {@link EntregaVista}.
 * Autorización: /graphql exige rol ADMIN (ConfiguracionLogistica).
 */
@Controller
class EntregasGraphQl {

    record FiltroEntregasInput(List<EstadoEntrega> estados, UUID entidadId, UUID donanteId, String patente,
                               String desde, String hasta, String texto) {
        FiltroEntregas aDominio() {
            return new FiltroEntregas(estados == null ? Set.of() : Set.copyOf(estados), entidadId, donanteId, patente,
                    instante("desde", desde), instante("hasta", hasta), texto);
        }

        private static Instant instante(String campo, String valor) {
            try {
                return valor == null || valor.isBlank() ? null : Instant.parse(valor);
            } catch (DateTimeParseException e) {
                throw new ReglaDeNegocioException("'" + campo + "' debe ser ISO-8601, por ejemplo 2026-11-01T00:00:00Z");
            }
        }
    }

    record PaginaEntregas(long total, int pagina, int tamanio, List<EntregaVista> elementos) {
    }

    private final ConsultasLogistica consultas;

    EntregasGraphQl(ConsultasLogistica consultas) {
        this.consultas = consultas;
    }

    @QueryMapping
    PaginaEntregas entregas(@Argument FiltroEntregasInput filtro, @Argument int pagina, @Argument int tamanio) {
        Pagina<Entrega> p = consultas.buscarEntregas(filtro == null ? FiltroEntregas.sinFiltros() : filtro.aDominio(),
                pagina, tamanio);
        return new PaginaEntregas(p.total(), p.numero(), p.tamanio(), p.elementos().stream().map(EntregaVista::de).toList());
    }
}
