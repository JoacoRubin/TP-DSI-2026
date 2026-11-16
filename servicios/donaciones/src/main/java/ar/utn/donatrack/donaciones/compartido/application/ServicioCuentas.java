package ar.utn.donatrack.donaciones.compartido.application;

import java.util.List;

/** Puerto de salida hacia el Servicio de Autenticación. */
public interface ServicioCuentas {

    CuentaCreada crear(NuevaCuenta cuenta);

    List<CuentaCreada> crearEnLote(List<NuevaCuenta> cuentas);

    /** @param clave null → la cuenta queda pendiente de activación y se devuelve un token. */
    record NuevaCuenta(String email, String nombre, String rol, String perfil, String referencia, String clave) {
    }

    record CuentaCreada(String email, boolean creada, String tokenActivacion) {
    }
}
