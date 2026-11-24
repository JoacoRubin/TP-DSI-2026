package ar.utn.donatrack.logistica.configuracion;

import ar.utn.donatrack.contratos.demo.DatosDemo.DonacionDemo;
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
import java.util.Map;
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

        // Una entrega por cada donación entregada de la historia compartida (mismos ids que Donaciones).
        List<DonacionDemo> entregadas = HISTORIA.stream().filter(DonacionDemo::entregada).toList();
        for (int i = 0; i < entregadas.size(); i++) {
            DonacionDemo d = entregadas.get(i);
            String foto = fotos.guardarSvg("demo-" + d.id() + ".svg", d.subcategoria(), COLORES[i % COLORES.length]);
            entregas.guardar(historica(d, foto));
        }
        log.info("Datos demo de logística: 2 camiones y {} entregas históricas", entregadas.size());
    }

    private static final String[] COLORES = {"#2563eb", "#059669", "#7c3aed", "#d97706", "#0f766e", "#db2777"};

    private static final Map<UUID, Destino> DESTINOS = Map.of(
            ENTIDAD_HOGAR, new Destino("Hogar de Niños Medrano", "Av. Díaz Vélez 4100, CABA", -34.6078, -58.4290),
            ENTIDAD_ESCUELA, new Destino("Escuela Rural N°10", "Ruta 6 km 32, Cañuelas", -35.0516, -58.7609),
            ENTIDAD_COMEDOR, new Destino("Comedor Escobar Sonrisas", "Tapia de Cruz 650, Belén de Escobar", -34.3489, -58.7956));

    private static final Map<UUID, String> DONANTES = Map.of(
            DONANTE_ANA, "Ana Pérez", DONANTE_ARCOS, "Arcos Plateados S.A.");

    private Entrega historica(DonacionDemo d, String foto) {
        Instant entregada = d.entregadaEn(reloj);
        return new Entrega(UUID.nameUUIDFromBytes(("entrega-" + d.id()).getBytes()), d.id(), d.donanteId(),
                DONANTES.get(d.donanteId()), d.entregadaA(), DESTINOS.get(d.entregadaA()),
                new DetalleDonacion(d.titulo(), d.subcategoria(), d.cantidad(), d.unidad()),
                20, 0.4, EstadoEntrega.ENTREGADA, null, CAMION_1, CAMION_1_PATENTE,
                List.of(foto), null, entregada, entregada.minus(Duration.ofDays(2)),
                List.of(new RegistroEstado(EstadoEntrega.ENTREGADA, entregada, "Recepción confirmada")));
    }
}
