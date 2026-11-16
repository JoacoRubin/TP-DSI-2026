package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.donaciones.compartido.domain.Textos;

/** Persona habilitada a operar en nombre de una persona jurídica. */
public record Representante(String nombre, String email) {
    public Representante {
        nombre = Textos.requerido(nombre, "nombre del representante");
        email = MediosContacto.validarEmail(email);
    }
}
