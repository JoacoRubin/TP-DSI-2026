package ar.utn.donatrack.soporte.seguridad;

/** Credenciales del servicio para el flujo client-credentials contra Autenticación. */
public record PropiedadesClienteServicio(String authUrl, String id, String secreto) {
}
