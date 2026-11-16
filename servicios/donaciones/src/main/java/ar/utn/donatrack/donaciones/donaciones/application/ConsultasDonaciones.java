package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.errores.OperacionNoPermitidaException;
import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.compartido.domain.Pagina;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.FiltroDonaciones;

import java.util.UUID;

/** Cada actor ve sólo lo suyo: el filtro se fuerza según quién consulta. */
public class ConsultasDonaciones {

    private final DonacionRepositorio donaciones;

    public ConsultasDonaciones(DonacionRepositorio donaciones) {
        this.donaciones = donaciones;
    }

    public Pagina<Donacion> buscar(FiltroDonaciones filtro, int pagina, int tamanio, Solicitante s) {
        FiltroDonaciones efectivo = filtro;
        if (!s.esAdmin()) {
            if ("DONANTE".equals(s.rol())) {
                efectivo = new FiltroDonaciones(s.referencia(), filtro.entidadId(), filtro.estado(), filtro.categoria(),
                        filtro.subcategoriaId());
            } else if ("BENEFICIARIA".equals(s.rol())) {
                efectivo = new FiltroDonaciones(filtro.donanteId(), s.referencia(), filtro.estado(), filtro.categoria(),
                        filtro.subcategoriaId());
            } else {
                throw new OperacionNoPermitidaException("No podés consultar donaciones");
            }
        }
        return donaciones.buscar(efectivo, pagina, Math.min(Math.max(tamanio, 1), 100));
    }

    public Donacion detalle(UUID id, Solicitante s) {
        Donacion d = donaciones.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Donación", id));
        if (!s.esAdmin() && !s.esDonante(d.donanteId()) && !s.esEntidad(d.entidadId())) {
            throw new OperacionNoPermitidaException("No podés ver esta donación");
        }
        return d;
    }
}
