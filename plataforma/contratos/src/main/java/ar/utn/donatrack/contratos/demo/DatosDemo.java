package ar.utn.donatrack.contratos.demo;

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

    private DatosDemo() {
    }
}
