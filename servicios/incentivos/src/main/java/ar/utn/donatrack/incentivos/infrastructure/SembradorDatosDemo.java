package ar.utn.donatrack.incentivos.infrastructure;

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
import java.util.UUID;

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
        ZoneId zona = reloj.getZone();
        YearMonth mes = YearMonth.now(reloj);
        sembrar(PerfilDonante.nuevo(DONANTE_ANA, "Ana Pérez", DONANTE_ANA_EMAIL, "+54 11 5555-5555", "+54 9 11 5555-5555",
                MedioNotificacion.EMAIL, true, reloj.instant()), mes, zona, 4, true);
        sembrar(PerfilDonante.nuevo(DONANTE_ARCOS, "Arcos Plateados S.A.", DONANTE_ARCOS_EMAIL, "+54 11 4444-4444", null,
                MedioNotificacion.EMAIL, false, reloj.instant()), mes, zona, 2, false);
        rankings.generar(mes.minusMonths(1));
        log.info("Datos demo de incentivos sembrados");
    }

    private void sembrar(PerfilDonante p, YearMonth mes, ZoneId zona, int meses, boolean conEntrega) {
        String[] categorias = {"Alimentos", "Abrigo y descanso", "Vestimenta", "Útiles escolares"};
        for (int i = meses; i >= 1; i--) {
            YearMonth m = mes.minusMonths(i);
            Instant fecha = m.atDay(12).atStartOfDay(zona).toInstant();
            ActividadDonacion a = ActividadDonacion.registrada(UUID.randomUUID(), p.donanteId(), UUID.randomUUID(),
                    categorias[i % categorias.length], "Varios", 10 * i, fecha);
            if (conEntrega || i == 1) {
                a.marcarEntregada(i % 2 == 0 ? ENTIDAD_HOGAR : ENTIDAD_ESCUELA, fecha.plusSeconds(86_400 * 3L));
            }
            actividades.guardar(a);
            // Se evalúa mes a mes para que las insignias queden fechadas en su período.
            HistorialDonante h = new HistorialDonante(actividades.deDonante(p.donanteId()), zona);
            insignias.guardarTodas(motor.evaluar(p, h, m, fecha.plusSeconds(86_400 * 4L)).insignias());
        }
        perfiles.guardar(p);
    }
}
