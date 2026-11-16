package ar.utn.donatrack.donaciones.beneficiarias.infrastructure.persistencia;

import ar.utn.donatrack.donaciones.beneficiarias.domain.EntidadBeneficiaria;
import ar.utn.donatrack.donaciones.compartido.infrastructure.persistencia.DireccionJpa;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "entidades_beneficiarias", indexes = {
        @Index(name = "ux_entidades_email", columnList = "emailPrincipal", unique = true)})
public class EntidadJpa {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String razonSocial;
    @Embedded
    private DireccionJpa direccion;
    private String telefono;
    /** Desnormalizado: el primer email, usado como identidad de la cuenta. */
    @Column(nullable = false)
    private String emailPrincipal;
    @ElementCollection
    @CollectionTable(name = "entidades_emails", joinColumns = @JoinColumn(name = "entidad_id"))
    @OrderColumn(name = "orden")
    @Column(name = "email")
    private List<String> emails = new ArrayList<>();
    private boolean activa;
    private Instant fechaAlta;

    protected EntidadJpa() {
    }

    static EntidadJpa desde(EntidadBeneficiaria e) {
        EntidadJpa j = new EntidadJpa();
        j.id = e.id();
        j.razonSocial = e.razonSocial();
        j.direccion = DireccionJpa.desde(e.direccion());
        j.telefono = e.telefono();
        j.emailPrincipal = e.emailPrincipal();
        j.emails = new ArrayList<>(e.emailsRepresentantes());
        j.activa = e.activa();
        j.fechaAlta = e.fechaAlta();
        return j;
    }

    EntidadBeneficiaria aDominio() {
        return new EntidadBeneficiaria(id, razonSocial, direccion.aDominio(), telefono, List.copyOf(emails), activa, fechaAlta);
    }
}
