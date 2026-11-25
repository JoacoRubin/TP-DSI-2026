package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadDonacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;

import java.time.Clock;

/** Integración Donaciones → Incentivos: cada donación impacta en el progreso de misiones. */
public class ProcesarDonacionRegistrada {

    private final ActividadRepositorio actividades;
    private final Repositorios.PerfilRepositorio perfiles;
    private final AplicadorProgreso aplicador;
    private final Clock reloj;

    public ProcesarDonacionRegistrada(ActividadRepositorio actividades, Repositorios.PerfilRepositorio perfiles,
                                      AplicadorProgreso aplicador, Clock reloj) {
        this.actividades = actividades;
        this.perfiles = perfiles;
        this.aplicador = aplicador;
        this.reloj = reloj;
    }

    public void ejecutar(EventosDonaciones.DonacionRegistrada e) {
        if (actividades.buscar(e.donacionId()).isPresent()) {
            return; // idempotencia: el broker garantiza "al menos una vez"
        }
        EventosDonaciones.DonanteResumen d = e.donante();
        PerfilDonante perfil = perfiles.buscar(d.id()).orElseGet(() -> PerfilDonante.nuevo(d.id(), d.nombre(), d.email(),
                d.telefono(), d.whatsapp(), d.medioPreferido(), d.personaHumana(), d.aceptaDifusionLogros(), reloj.instant()));
        perfil.actualizarContacto(d.nombre(), d.email(), d.telefono(), d.whatsapp(), d.medioPreferido());
        actividades.guardar(ActividadDonacion.registrada(e.donacionId(), d.id(), e.cargaId(), e.categoria(),
                e.subcategoria(), e.cantidad(), e.registradaEn()));
        aplicador.aplicar(perfil);
    }
}
