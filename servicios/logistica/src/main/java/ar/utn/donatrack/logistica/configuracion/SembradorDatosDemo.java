package ar.utn.donatrack.logistica.configuracion;

import ar.utn.donatrack.logistica.compartido.infrastructure.AlmacenFotos;
import ar.utn.donatrack.logistica.entregas.domain.DetalleDonacion;
import ar.utn.donatrack.logistica.entregas.domain.Destino;
import ar.utn.donatrack.logistica.entregas.domain.Entrega;
import ar.utn.donatrack.logistica.entregas.domain.EntregaRepositorio;
import ar.utn.donatrack.logistica.entregas.domain.EstadoEntrega;
import ar.utn.donatrack.logistica.entregas.domain.RegistroEstado;
import ar.utn.donatrack.logistica.flota.domain.Camion;
import ar.utn.donatrack.logistica.flota.domain.CamionRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.contratos.demo.DatosDemo.*;

/** Flota demo + entregas históricas (coinciden con las donaciones históricas de Donaciones). */
@Component
@Profile("local")
class SembradorDatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorDatosDemo.class);
    private final CamionRepositorio camiones;
    private final EntregaRepositorio entregas;
    private final AlmacenFotos fotos;
    private final Clock reloj;

    SembradorDatosDemo(CamionRepositorio camiones, EntregaRepositorio entregas, AlmacenFotos fotos, Clock reloj) {
        this.camiones = camiones;
        this.entregas = entregas;
        this.fotos = fotos;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (camiones.buscar(CAMION_1).isPresent()) {
            return;
        }
        camiones.guardar(Camion.registrar(CAMION_1, CAMION_1_PATENTE, 18, 3.2, 3500));
        camiones.guardar(Camion.registrar(CAMION_2, CAMION_2_PATENTE, 30, 3.8, 7000));

        Instant ahora = reloj.instant();
        entregas.guardar(historica(DONACION_HISTORICA_1, DONANTE_ANA, "Ana Pérez", ENTIDAD_HOGAR,
                new Destino("Hogar de Niños Medrano", "Av. Díaz Vélez 4100, CABA", -34.6078, -58.4290),
                new DetalleDonacion("20 unidades de Frazadas", "Frazadas", 20, "unidades"),
                ahora.minus(Duration.ofDays(5)), fotos.guardarSvg("demo-frazadas.svg", "Frazadas recibidas", "#2563eb")));
        entregas.guardar(historica(DONACION_HISTORICA_2, DONANTE_ARCOS, "Arcos Plateados S.A.", ENTIDAD_ESCUELA,
                new Destino("Escuela Rural N°10", "Ruta 6 km 32, Cañuelas", -35.0516, -58.7609),
                new DetalleDonacion("10 unidades de Bancos escolares", "Bancos escolares", 10, "unidades"),
                ahora.minus(Duration.ofDays(12)), fotos.guardarSvg("demo-bancos.svg", "Bancos para el aula", "#059669")));
        log.info("Datos demo de logística: 2 camiones y 2 entregas históricas");
    }

    private Entrega historica(UUID donacionId, UUID donanteId, String donante, UUID entidadId, Destino destino,
                              DetalleDonacion detalle, Instant entregada, String foto) {
        return new Entrega(UUID.nameUUIDFromBytes(("entrega-" + donacionId).getBytes()), donacionId, donanteId, donante,
                entidadId, destino, detalle, 20, 0.4, EstadoEntrega.ENTREGADA, null, CAMION_1, CAMION_1_PATENTE,
                List.of(foto), null, entregada, entregada.minus(Duration.ofDays(2)),
                List.of(new RegistroEstado(EstadoEntrega.ENTREGADA, entregada, "Recepción confirmada")));
    }
}
