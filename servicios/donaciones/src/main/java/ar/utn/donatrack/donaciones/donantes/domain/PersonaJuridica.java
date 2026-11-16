package ar.utn.donatrack.donaciones.donantes.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.compartido.domain.Direccion;
import ar.utn.donatrack.donaciones.compartido.domain.Textos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PersonaJuridica extends Donante {

    private String razonSocial;
    private TipoPersonaJuridica tipoOrganizacion;
    private String rubro;
    private Documento cuit;
    private final List<Representante> representantes;

    public PersonaJuridica(UUID id, String razonSocial, TipoPersonaJuridica tipoOrganizacion, String rubro, Documento cuit,
                           Direccion direccion, MediosContacto contacto, List<Representante> representantes,
                           boolean activo, Instant fechaAlta, Instant ultimaInteraccion, Instant ultimoAvisoInactividad) {
        super(id, contacto, direccion, activo, fechaAlta, ultimaInteraccion, ultimoAvisoInactividad);
        this.razonSocial = Textos.requerido(razonSocial, "razón social");
        this.tipoOrganizacion = tipoOrganizacion;
        this.rubro = rubro;
        this.cuit = Objects.requireNonNull(cuit);
        this.representantes = new ArrayList<>(representantes == null ? List.of() : representantes);
    }

    public static PersonaJuridica registrar(UUID id, String razonSocial, TipoPersonaJuridica tipo, String rubro,
                                            Documento cuit, Direccion direccion, MediosContacto contacto,
                                            List<Representante> representantes, Instant ahora) {
        if (tipo == null) {
            throw new ReglaDeNegocioException("El tipo de organización es obligatorio");
        }
        Textos.requerido(rubro, "rubro");
        return new PersonaJuridica(id, razonSocial, tipo, rubro, cuit, direccion, contacto, representantes, true, ahora, ahora, null);
    }

    public static PersonaJuridica importada(UUID id, String razonSocial, Documento cuit, MediosContacto contacto, Instant ahora) {
        return new PersonaJuridica(id, razonSocial, null, null, cuit, null, contacto, List.of(), true, ahora, ahora, null);
    }

    public void actualizarDatos(String razonSocial, TipoPersonaJuridica tipo, String rubro, Documento cuit) {
        this.razonSocial = Textos.requerido(razonSocial, "razón social");
        if (tipo != null) {
            this.tipoOrganizacion = tipo;
        }
        if (rubro != null) {
            this.rubro = rubro;
        }
        if (cuit != null) {
            this.cuit = cuit;
        }
    }

    public void agregarRepresentante(Representante r) {
        if (representantes.stream().anyMatch(x -> x.email().equals(r.email()))) {
            throw new ReglaDeNegocioException("Ya existe un representante con ese email");
        }
        representantes.add(r);
    }

    @Override
    public String nombreVisible() {
        return razonSocial;
    }

    @Override
    public TipoDonante tipo() {
        return TipoDonante.JURIDICA;
    }

    @Override
    public Documento documento() {
        return cuit;
    }

    public String razonSocial() { return razonSocial; }
    public TipoPersonaJuridica tipoOrganizacion() { return tipoOrganizacion; }
    public String rubro() { return rubro; }
    public List<Representante> representantes() { return List.copyOf(representantes); }
}
