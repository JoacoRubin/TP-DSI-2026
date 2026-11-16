package ar.utn.donatrack.autenticacion.usuarios.application;

/** Puerto: algoritmo de hash de contraseñas (adaptador: BCrypt). */
public interface HasheadorClaves {
    String hashear(String clave);

    boolean coincide(String clave, String hash);
}
