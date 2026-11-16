package ar.utn.donatrack.donaciones.beneficiarias.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Necesidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Periodicidad;
import ar.utn.donatrack.donaciones.beneficiarias.domain.TipoNecesidad;
import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.catalogo.domain.Subcategoria;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;

import java.time.Clock;
import java.util.UUID;

public class RegistrarNecesidad {

    public record Comando(UUID entidadId, TipoNecesidad tipo, UUID subcategoriaId, String descripcion, int cantidad,
                          String unidad, Periodicidad periodicidad) {
    }

    private final EntidadRepositorio entidades;
    private final NecesidadRepositorio necesidades;
    private final CatalogoRepositorio catalogo;
    private final Clock reloj;

    public RegistrarNecesidad(EntidadRepositorio entidades, NecesidadRepositorio necesidades,
                              CatalogoRepositorio catalogo, Clock reloj) {
        this.entidades = entidades;
        this.necesidades = necesidades;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    public Necesidad ejecutar(Comando c, Solicitante solicitante) {
        solicitante.exigirAdminOEntidad(c.entidadId());
        EntidadBeneficiaria entidad = entidades.buscar(c.entidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Entidad", c.entidadId()));
        if (!entidad.activa()) {
            throw new ReglaDeNegocioException("La entidad está dada de baja");
        }
        Subcategoria sub = catalogo.catalogo().subcategoria(c.subcategoriaId())
                .orElseThrow(() -> new ReglaDeNegocioException("Subcategoría inexistente"));
        if (c.tipo() == null) {
            throw new ReglaDeNegocioException("Indicá si la necesidad es recurrente o extraordinaria");
        }
        Necesidad necesidad = switch (c.tipo()) {
            case EXTRAORDINARIA -> NecesidadExtraordinaria.nueva(UUID.randomUUID(), entidad.id(), sub.id(), sub.nombre(),
                    c.descripcion(), c.cantidad(), c.unidad(), reloj.instant());
            case RECURRENTE -> NecesidadRecurrente.nueva(UUID.randomUUID(), entidad.id(), sub.id(), sub.nombre(),
                    c.descripcion(), c.cantidad(), c.unidad(),
                    c.periodicidad() == null ? Periodicidad.MENSUAL : c.periodicidad(), reloj.instant());
        };
        necesidades.guardar(necesidad);
        return necesidad;
    }
}
