package ar.utn.donatrack.autenticacion.usuarios.application;

public record TokenEmitido(String accessToken, long expiraEnSegundos) {
}
