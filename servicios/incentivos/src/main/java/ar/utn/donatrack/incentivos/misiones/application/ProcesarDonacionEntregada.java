package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;

/** Una entrega exitosa suma a "Donaciones exitosas" y a "organizaciones ayudadas". */
public class ProcesarDonacionEntregada {

    private final ActividadRepositorio actividades;
    private final Repositorios.PerfilRepositorio perfiles;
    private final AplicadorProgreso aplicador;

    public ProcesarDonacionEntregada(ActividadRepositorio actividades, Repositorios.PerfilRepositorio perfiles,
                                     AplicadorProgreso aplicador) {
        this.actividades = actividades;
        this.perfiles = perfiles;
        this.aplicador = aplicador;
    }

    public void ejecutar(EventosDonaciones.DonacionEntregada e) {
        actividades.buscar(e.donacionId()).filter(a -> !a.entregada()).ifPresent(a -> {
            a.marcarEntregada(e.entidadId(), e.entregadaEn());
            actividades.guardar(a);
            perfiles.buscar(e.donanteId()).ifPresent(aplicador::aplicar);
        });
    }
}
