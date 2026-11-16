package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.errores.RecursoNoEncontradoException;
import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.compartido.application.PublicadorEventos;
import ar.utn.donatrack.donaciones.compartido.application.Solicitante;
import ar.utn.donatrack.donaciones.donaciones.domain.CargaDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.Segmentador;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * La administración carga TODOS los bienes en una única operación; el sistema segmenta
 * automáticamente en donaciones independientes por subcategoría.
 */
public class RegistrarCargaDonacion {

    private final DonanteRepositorio donantes;
    private final DonacionRepositorio donaciones;
    private final CatalogoRepositorio catalogo;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public RegistrarCargaDonacion(DonanteRepositorio donantes, DonacionRepositorio donaciones,
                                  CatalogoRepositorio catalogo, PublicadorEventos eventos, Clock reloj) {
        this.donantes = donantes;
        this.donaciones = donaciones;
        this.catalogo = catalogo;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    public List<Donacion> ejecutar(CargaDonacion carga, Solicitante solicitante) {
        Donante donante = donantes.buscar(carga.donanteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Donante", carga.donanteId()));
        if (!donante.activo()) {
            throw new ReglaDeNegocioException("La persona donante está dada de baja");
        }
        Instant ahora = reloj.instant();
        List<Donacion> resultado = new Segmentador(catalogo.catalogo())
                .segmentar(carga, solicitante.usuario(), LocalDate.now(reloj), ahora);
        donaciones.guardarTodas(resultado);
        donante.registrarInteraccion(ahora);
        donantes.guardar(donante);

        EventosDonaciones.DonanteResumen resumen = ResumenDonante.de(donante);
        resultado.forEach(d -> eventos.donacionRegistrada(new EventosDonaciones.DonacionRegistrada(
                d.id(), d.cargaId(), resumen, d.categoriaNombre(), d.subcategoriaNombre(), d.cantidadTotal(),
                d.registradaEn())));
        return resultado;
    }
}
