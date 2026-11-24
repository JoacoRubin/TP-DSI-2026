package ar.utn.donatrack.contratos.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Identificadores fijos de los datos de demostración (perfil "local"). Cada servicio siembra
 * SU parte de los datos; estos IDs garantizan que las referencias entre servicios coincidan.
 */
public final class DatosDemo {

    public static final String CLAVE_DEMO = "Demo1234!";

    public static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-00000000a001");
    public static final String ADMIN_EMAIL = "admin@donatrack.org";

    public static final UUID CHOFER = UUID.fromString("00000000-0000-0000-0000-00000000c001");
    public static final String CHOFER_EMAIL = "chofer@donatrack.org";

    public static final UUID CAMION_1 = UUID.fromString("00000000-0000-0000-0000-0000000ca001");
    public static final String CAMION_1_PATENTE = "AE123BC";
    public static final UUID CAMION_2 = UUID.fromString("00000000-0000-0000-0000-0000000ca002");
    public static final String CAMION_2_PATENTE = "AF456DE";

    public static final UUID DONANTE_ANA = UUID.fromString("00000000-0000-0000-0000-0000000d0001");
    public static final String DONANTE_ANA_EMAIL = "ana@donatrack.org";
    public static final UUID DONANTE_ARCOS = UUID.fromString("00000000-0000-0000-0000-0000000d0002");
    public static final String DONANTE_ARCOS_EMAIL = "contacto@arcosplateados.com";

    public static final UUID ENTIDAD_ESCUELA = UUID.fromString("00000000-0000-0000-0000-0000000e0001");
    public static final String ENTIDAD_ESCUELA_EMAIL = "escuela10@donatrack.org";
    public static final UUID ENTIDAD_COMEDOR = UUID.fromString("00000000-0000-0000-0000-0000000e0002");
    public static final String ENTIDAD_COMEDOR_EMAIL = "comedor@donatrack.org";
    public static final UUID ENTIDAD_HOGAR = UUID.fromString("00000000-0000-0000-0000-0000000e0003");
    public static final String ENTIDAD_HOGAR_EMAIL = "hogar@donatrack.org";

    /** Donaciones históricas ya entregadas (aparecen en la landing, galería y mapa público). */
    public static final UUID DONACION_HISTORICA_1 = UUID.fromString("00000000-0000-0000-0000-0000000f0001");
    public static final UUID DONACION_HISTORICA_2 = UUID.fromString("00000000-0000-0000-0000-0000000f0002");

    /**
     * Una donación de la historia demo. Donaciones la crea, Incentivos registra su actividad con el
     * MISMO id y Logística crea su entrega si está entregada: así el panel del donante, su listado,
     * sus insignias y el mapa público cuentan lo mismo.
     *
     * @param mesesAtras si es mayor que 0, se registró el día 12 de ese mes; si no, hace {@code diasAtras} días
     * @param usado      sólo importa en categorías que exigen estado de uso
     * @param venceEnMeses sólo para perecederos (null si no lo es)
     * @param entregadaA entidad que la recibió, o null si sigue en el depósito
     */
    public record DonacionDemo(UUID id, UUID cargaId, UUID donanteId, int mesesAtras, int diasAtras,
                               String categoria, String subcategoria, String bien, int cantidad, String unidad,
                               boolean usado, Integer venceEnMeses, UUID entregadaA) {

        public Instant registradaEn(Clock reloj) {
            if (mesesAtras > 0) {
                return YearMonth.now(reloj).minusMonths(mesesAtras).atDay(12).atStartOfDay(reloj.getZone()).toInstant();
            }
            return reloj.instant().minus(Duration.ofDays(diasAtras));
        }

        public Instant entregadaEn(Clock reloj) {
            return registradaEn(reloj).plus(Duration.ofDays(3));
        }

        public boolean entregada() {
            return entregadaA != null;
        }

        public String titulo() {
            return cantidad + " " + unidad + " de " + subcategoria;
        }
    }

    private static UUID id(String sufijo) {
        return UUID.fromString("00000000-0000-0000-0000-0000000" + sufijo);
    }

    private static final UUID CARGA_ANA_ALIMENTOS = id("b0001");
    private static final UUID CARGA_ARCOS_MUDANZA = id("b0002");

    /** Toda la historia demo, en orden cronológico por donante. */
    public static final List<DonacionDemo> HISTORIA = List.of(
            // Ana: una donación por mes desde julio. Cada mes completa una misión (en orden): jul Primer paso +
            // Manos que llegan, ago Constancia, sep Versátil, oct Hábil donador/a (>50 bienes), nov Impacto real
            // (5 recibidas). Así el ranking del mes cerrado (lo que muestra la portada) no queda vacío.
            new DonacionDemo(id("f0003"), id("b0103"), DONANTE_ANA, 4, 0, "Vestimenta", "Ropa infantil",
                    "Conjuntos de ropa infantil", 15, "unidades", false, null, ENTIDAD_HOGAR),
            new DonacionDemo(id("f0004"), id("b0104"), DONANTE_ANA, 3, 0, "Útiles escolares", "Cuadernos",
                    "Cuadernos rayados", 40, "unidades", false, null, ENTIDAD_ESCUELA),
            new DonacionDemo(id("f0005"), id("b0105"), DONANTE_ANA, 2, 0, "Abrigo y descanso", "Sábanas",
                    "Juegos de sábanas", 10, "unidades", false, null, ENTIDAD_HOGAR),
            new DonacionDemo(id("f0006"), id("b0106"), DONANTE_ANA, 1, 0, "Útiles escolares", "Mochilas",
                    "Mochilas escolares", 60, "unidades", false, null, ENTIDAD_ESCUELA),
            // ... las frazadas de este mes, ya entregadas ...
            new DonacionDemo(DONACION_HISTORICA_1, id("b0101"), DONANTE_ANA, 0, 8, "Abrigo y descanso", "Frazadas",
                    "Frazadas de abrigo", 20, "unidades", false, null, ENTIDAD_HOGAR),
            // ... y una carga de alimentos de hoy (más de 50 bienes), en el depósito.
            new DonacionDemo(id("f0011"), CARGA_ANA_ALIMENTOS, DONANTE_ANA, 0, 0, "Alimentos", "Fideos secos",
                    "Fideos secos tirabuzón", 100, "paquetes", false, 6, null),
            new DonacionDemo(id("f0012"), CARGA_ANA_ALIMENTOS, DONANTE_ANA, 0, 0, "Alimentos", "Puré de tomate",
                    "Puré de tomate tetra-pack", 50, "unidades", false, 4, null),
            // Arcos Plateados: bancos entregados y la mudanza de la oficina en el depósito.
            new DonacionDemo(DONACION_HISTORICA_2, id("b0102"), DONANTE_ARCOS, 0, 15, "Mobiliario", "Bancos escolares",
                    "Bancos de madera", 10, "unidades", true, null, ENTIDAD_ESCUELA),
            new DonacionDemo(id("f0013"), CARGA_ARCOS_MUDANZA, DONANTE_ARCOS, 0, 0, "Mobiliario", "Sillas",
                    "Silla de oficina", 6, "unidades", true, null, null),
            new DonacionDemo(id("f0014"), CARGA_ARCOS_MUDANZA, DONANTE_ARCOS, 0, 0, "Mobiliario", "Mesas",
                    "Mesa rectangular", 1, "unidades", true, null, null));

    private DatosDemo() {
    }
}
