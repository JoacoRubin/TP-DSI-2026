package ar.utn.donatrack.donaciones.configuracion;

import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadExtraordinaria;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRecurrente;
import ar.utn.donatrack.donaciones.beneficiarias.domain.NecesidadRepositorio;
import ar.utn.donatrack.donaciones.beneficiarias.domain.Periodicidad;
import ar.utn.donatrack.donaciones.catalogo.domain.Catalogo;
import ar.utn.donatrack.donaciones.catalogo.domain.CatalogoRepositorio;
import ar.utn.donatrack.donaciones.catalogo.domain.Categoria;
import ar.utn.donatrack.donaciones.catalogo.domain.Subcategoria;
import ar.utn.donatrack.donaciones.compartido.domain.Coordenadas;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.donaciones.domain.Bien;
import ar.utn.donatrack.donaciones.donaciones.domain.BienDeclarado;
import ar.utn.donatrack.donaciones.donaciones.domain.CambioEstado;
import ar.utn.donatrack.donaciones.donaciones.domain.CargaDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.Donacion;
import ar.utn.donatrack.donaciones.donaciones.domain.DonacionRepositorio;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoDonacion;
import ar.utn.donatrack.donaciones.donaciones.domain.EstadoUso;
import ar.utn.donatrack.donaciones.donaciones.domain.Segmentador;
import ar.utn.donatrack.donaciones.donantes.domain.Documento;
import ar.utn.donatrack.donaciones.donantes.domain.DonanteRepositorio;
import ar.utn.donatrack.donaciones.donantes.domain.Genero;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaJuridica;
import ar.utn.donatrack.donaciones.donantes.domain.Representante;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoPersonaJuridica;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static ar.utn.donatrack.contratos.demo.DatosDemo.*;

/**
 * Datos de demostración del perfil "local". Se siembran directo por repositorio (sin pasar por
 * Autenticación): las cuentas las siembra cada servicio con los mismos IDs de {@code DatosDemo}.
 */
@Component
@Profile("local")
class SembradorDatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorDatosDemo.class);

    private final CatalogoRepositorio catalogo;
    private final DonanteRepositorio donantes;
    private final EntidadRepositorio entidades;
    private final NecesidadRepositorio necesidades;
    private final DonacionRepositorio donaciones;
    private final Clock reloj;

    SembradorDatosDemo(CatalogoRepositorio catalogo, DonanteRepositorio donantes, EntidadRepositorio entidades,
                       NecesidadRepositorio necesidades, DonacionRepositorio donaciones, Clock reloj) {
        this.catalogo = catalogo;
        this.donantes = donantes;
        this.entidades = entidades;
        this.necesidades = necesidades;
        this.donaciones = donaciones;
        this.reloj = reloj;
    }

    private static Subcategoria sub(String nombre) {
        return new Subcategoria(UUID.nameUUIDFromBytes(("sub-" + nombre).getBytes()), nombre);
    }

    private static Categoria cat(String nombre, boolean perecedera, boolean estadoUso, String... subs) {
        return new Categoria(UUID.nameUUIDFromBytes(("cat-" + nombre).getBytes()), nombre, perecedera, estadoUso,
                java.util.Arrays.stream(subs).map(SembradorDatosDemo::sub).toList());
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!catalogo.estaVacio()) {
            return;
        }
        Instant ahora = reloj.instant();
        List.of(
                cat("Alimentos", true, false, "Fideos secos", "Arroz", "Legumbres secas", "Aceite vegetal", "Puré de tomate", "Leche en polvo"),
                cat("Vestimenta", false, true, "Camperas de abrigo", "Remeras", "Pantalones", "Ropa infantil", "Calzado"),
                cat("Mobiliario", false, true, "Sillas", "Mesas", "Bancos escolares", "Estanterías"),
                cat("Abrigo y descanso", false, true, "Frazadas", "Colchones", "Sábanas"),
                cat("Útiles escolares", false, false, "Cuadernos", "Lápices y lapiceras", "Mochilas"),
                cat("Higiene", true, false, "Jabón", "Pañales", "Artículos de limpieza")
        ).forEach(catalogo::guardar);
        Catalogo c = catalogo.catalogo();

        Direccion utn = new Direccion("Medrano", "951", "CABA", "Buenos Aires", "C1179AAQ", new Coordenadas(-34.5986, -58.4202));
        donantes.guardar(PersonaHumana.registrar(DONANTE_ANA, "Ana", "Pérez", 34, new Documento(TipoDocumento.DNI, "30123456"),
                Genero.FEMENINO, utn, new MediosContacto(DONANTE_ANA_EMAIL, "+54 11 5555-5555", "+54 9 11 5555-5555",
                        MedioNotificacion.EMAIL), ahora.minus(Duration.ofDays(120))));
        donantes.guardar(PersonaJuridica.registrar(DONANTE_ARCOS, "Arcos Plateados S.A.", TipoPersonaJuridica.EMPRESA,
                "Gastronomía", new Documento(TipoDocumento.CUIT, "30-71234567-8"),
                new Direccion("Av. Corrientes", "3200", "CABA", "Buenos Aires", "C1193AAF", new Coordenadas(-34.6045, -58.4108)),
                new MediosContacto(DONANTE_ARCOS_EMAIL, "+54 11 4444-4444", null, MedioNotificacion.EMAIL),
                List.of(new Representante("Laura Gómez", "laura.gomez@arcosplateados.com")), ahora.minus(Duration.ofDays(60))));

        EntidadBeneficiaria escuela = EntidadBeneficiaria.registrar(ENTIDAD_ESCUELA, "Escuela Rural N°10",
                new Direccion("Ruta 6", "km 32", "Cañuelas", "Buenos Aires", "1814", new Coordenadas(-35.0516, -58.7609)),
                "+54 2226 43-1010", List.of(ENTIDAD_ESCUELA_EMAIL), ahora);
        EntidadBeneficiaria comedor = EntidadBeneficiaria.registrar(ENTIDAD_COMEDOR, "Comedor Escobar Sonrisas",
                new Direccion("Tapia de Cruz", "650", "Belén de Escobar", "Buenos Aires", "1625", new Coordenadas(-34.3489, -58.7956)),
                "+54 348 442-2020", List.of(ENTIDAD_COMEDOR_EMAIL), ahora);
        EntidadBeneficiaria hogar = EntidadBeneficiaria.registrar(ENTIDAD_HOGAR, "Hogar de Niños Medrano",
                new Direccion("Av. Díaz Vélez", "4100", "CABA", "Buenos Aires", "C1200", new Coordenadas(-34.6078, -58.4290)),
                "+54 11 4981-3030", List.of(ENTIDAD_HOGAR_EMAIL), ahora);
        List.of(escuela, comedor, hogar).forEach(entidades::guardar);

        necesidades.guardar(NecesidadExtraordinaria.nueva(UUID.randomUUID(), escuela.id(), sub("Sillas").id(), "Sillas",
                "Sillas para aula inundada", 30, "unidades", ahora));
        necesidades.guardar(NecesidadExtraordinaria.nueva(UUID.randomUUID(), escuela.id(), sub("Bancos escolares").id(),
                "Bancos escolares", "Bancos para reponer pérdida por inundación", 30, "unidades", ahora));
        necesidades.guardar(NecesidadRecurrente.nueva(UUID.randomUUID(), comedor.id(), sub("Fideos secos").id(), "Fideos secos",
                "Paquetes de fideos secos", 100, "paquetes", Periodicidad.SEMANAL, ahora));
        necesidades.guardar(NecesidadRecurrente.nueva(UUID.randomUUID(), hogar.id(), sub("Fideos secos").id(), "Fideos secos",
                "Fideos para el almuerzo", 40, "paquetes", Periodicidad.SEMANAL, ahora));
        necesidades.guardar(NecesidadExtraordinaria.nueva(UUID.randomUUID(), hogar.id(), sub("Frazadas").id(), "Frazadas",
                "Frazadas para el invierno", 25, "unidades", ahora));
        necesidades.guardar(NecesidadExtraordinaria.nueva(UUID.randomUUID(), comedor.id(), sub("Sillas").id(), "Sillas",
                "Sillas para el salón comedor", 12, "unidades", ahora));

        // Donaciones en depósito, listas para el matchmaking
        Segmentador segmentador = new Segmentador(c);
        LocalDate hoy = LocalDate.now(reloj);
        donaciones.guardarTodas(segmentador.segmentar(new CargaDonacion(DONANTE_ARCOS, "Mudanza de oficina corporativa", List.of(
                new BienDeclarado("Silla de oficina", sub("Sillas").id(), 6, "unidades", null, EstadoUso.USADO, null, 30.0, 1.2),
                new BienDeclarado("Mesa rectangular", sub("Mesas").id(), 1, "unidades", null, EstadoUso.USADO, null, 40.0, 1.5))),
                ADMIN_EMAIL, hoy, ahora));
        donaciones.guardarTodas(segmentador.segmentar(new CargaDonacion(DONANTE_ANA, "Donación de alimentos", List.of(
                new BienDeclarado("Fideos secos tirabuzón", sub("Fideos secos").id(), 100, "paquetes", hoy.plusMonths(6), null, null, 50.0, 0.3),
                new BienDeclarado("Puré de tomate tetra-pack", sub("Puré de tomate").id(), 50, "unidades", hoy.plusMonths(4), null, null, 25.0, 0.1))),
                ADMIN_EMAIL, hoy, ahora));

        // Historial: dos donaciones ya entregadas (alimentan landing, galería y mapa público)
        donaciones.guardar(historica(DONACION_HISTORICA_1, DONANTE_ANA, sub("Frazadas"), "Frazadas", "Abrigo y descanso",
                "Frazadas de abrigo", 20, ENTIDAD_HOGAR, ahora.minus(Duration.ofDays(5))));
        donaciones.guardar(historica(DONACION_HISTORICA_2, DONANTE_ARCOS, sub("Bancos escolares"), "Bancos escolares",
                "Mobiliario", "Bancos de madera", 10, ENTIDAD_ESCUELA, ahora.minus(Duration.ofDays(12))));
        log.info("Datos demo sembrados: catálogo, 2 donantes, 3 entidades, necesidades y donaciones");
    }

    private Donacion historica(UUID id, UUID donante, Subcategoria s, String subNombre, String categoria, String bien,
                               int cantidad, UUID entidad, Instant entregada) {
        Instant registrada = entregada.minus(Duration.ofDays(3));
        List<CambioEstado> historial = List.of(
                new CambioEstado(null, EstadoDonacion.EN_DEPOSITO, registrada, ADMIN_EMAIL, "Ingreso al depósito"),
                new CambioEstado(EstadoDonacion.EN_DEPOSITO, EstadoDonacion.ASIGNADA, registrada.plus(Duration.ofDays(1)), ADMIN_EMAIL, null),
                new CambioEstado(EstadoDonacion.ASIGNADA, EstadoDonacion.LISTA_PARA_ENTREGAR, registrada.plus(Duration.ofDays(2)), "logistica", null),
                new CambioEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, EstadoDonacion.EN_TRASLADO, entregada.minus(Duration.ofHours(3)), "logistica", null),
                new CambioEstado(EstadoDonacion.EN_TRASLADO, EstadoDonacion.ENTREGADA, entregada, "entidad", "Recepción confirmada"));
        return new Donacion(id, UUID.randomUUID(), donante, "Donación histórica", s.id(), subNombre, categoria, "unidades",
                null, EstadoUso.NUEVO, List.of(new Bien(bien, cantidad, null)), cantidad * 1.0, cantidad * 0.02,
                EstadoDonacion.ENTREGADA, null, entidad, CAMION_1_PATENTE, entregada, registrada, historial);
    }
}
