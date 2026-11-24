package ar.utn.donatrack.incentivos.infrastructure;

import ar.utn.donatrack.contratos.demo.DatosDemo.DonacionDemo;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadDonacion;
import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;
import ar.utn.donatrack.incentivos.misiones.domain.MotorIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;
import ar.utn.donatrack.incentivos.ranking.application.Rankings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

import static ar.utn.donatrack.contratos.demo.DatosDemo.*;

/** Historial demo para que el panel de incentivos y el ranking tengan datos desde el inicio. */
@Component
@Profile("local")
class SembradorDatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorDatosDemo.class);
    private final Repositorios.PerfilRepositorio perfiles;
    private final Repositorios.InsigniaRepositorio insignias;
    private final ActividadRepositorio actividades;
    private final MotorIncentivos motor;
    private final Rankings rankings;
    private final Clock reloj;

    SembradorDatosDemo(Repositorios.PerfilRepositorio perfiles, Repositorios.InsigniaRepositorio insignias,
                       ActividadRepositorio actividades, MotorIncentivos motor, Rankings rankings, Clock reloj) {
        this.perfiles = perfiles;
        this.insignias = insignias;
        this.actividades = actividades;
        this.motor = motor;
        this.rankings = rankings;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (perfiles.buscar(DONANTE_ANA).isPresent()) {
            return;
        }
        sembrar(PerfilDonante.nuevo(DONANTE_ANA, "Ana Pérez", DONANTE_ANA_EMAIL, "+54 11 5555-5555", "+54 9 11 5555-5555",
                MedioNotificacion.EMAIL, true, reloj.instant()));
        sembrar(PerfilDonante.nuevo(DONANTE_ARCOS, "Arcos Plateados S.A.", DONANTE_ARCOS_EMAIL, "+54 11 4444-4444", null,
                MedioNotificacion.EMAIL, false, reloj.instant()));
        rankings.generar(YearMonth.now(reloj).minusMonths(1));
        log.info("Datos demo de incentivos sembrados");
    }

    /**
     * Misma historia que siembran Donaciones y Logística ({@code DatosDemo.HISTORIA}, mismos ids): lo que el
     * panel cuenta es lo que el donante ve en "Mis donaciones". Se evalúa donación por donación, en orden,
     * para que cada insignia quede fechada cuando se ganó.
     */
    private void sembrar(PerfilDonante p) {
        ZoneId zona = reloj.getZone();
        for (DonacionDemo d : HISTORIA) {
            if (!d.donanteId().equals(p.donanteId())) {
                continue;
            }
            Instant registrada = d.registradaEn(reloj);
            ActividadDonacion a = ActividadDonacion.registrada(d.id(), p.donanteId(), d.cargaId(), d.categoria(),
                    d.subcategoria(), d.cantidad(), registrada);
            Instant evaluacion = registrada;
            if (d.entregada()) {
                a.marcarEntregada(d.entregadaA(), d.entregadaEn(reloj));
                evaluacion = d.entregadaEn(reloj);
            }
            actividades.guardar(a);
            HistorialDonante h = new HistorialDonante(actividades.deDonante(p.donanteId()), zona);
            insignias.guardarTodas(motor.evaluar(p, h, YearMonth.from(evaluacion.atZone(zona)), evaluacion).insignias());
        }
        perfiles.guardar(p);
    }
}
