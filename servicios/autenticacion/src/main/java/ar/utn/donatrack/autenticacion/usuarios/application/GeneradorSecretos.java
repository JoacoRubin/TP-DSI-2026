package ar.utn.donatrack.autenticacion.usuarios.application;

/** Puerto: genera secretos aleatorios criptográficamente seguros (tokens de activación). */
@FunctionalInterface
public interface GeneradorSecretos {
    String nuevo();
}
