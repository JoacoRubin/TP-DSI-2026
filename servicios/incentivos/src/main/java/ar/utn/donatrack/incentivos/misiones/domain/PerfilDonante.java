package ar.utn.donatrack.incentivos.misiones.domain;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;
import ar.utn.donatrack.contratos.notificaciones.MedioNotificacion;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Estado de gamificación de un donante: categoría y misión en curso. Los datos de contacto son
 * una copia desnormalizada (llegan en los eventos) para poder notificar sin consultar a Donaciones.
 */
public class PerfilDonante {

    private final UUID donanteId;
    private String nombre;
    private String email;
    private String telefono;
    private String whatsapp;
    private MedioNotificacion medioPreferido;
    private boolean personaHumana;
    private CategoriaDonante categoria;
    private int indiceMision;
    private boolean insigniasVisibles;
    private final Instant creadoEn;

    public PerfilDonante(UUID donanteId, String nombre, String email, String telefono, String whatsapp,
                         MedioNotificacion medioPreferido, boolean personaHumana, CategoriaDonante categoria,
                         int indiceMision, boolean insigniasVisibles, Instant creadoEn) {
        this.donanteId = Objects.requireNonNull(donanteId);
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.whatsapp = whatsapp;
        this.medioPreferido = medioPreferido;
        this.personaHumana = personaHumana;
        this.categoria = categoria;
        this.indiceMision = indiceMision;
        this.insigniasVisibles = insigniasVisibles;
        this.creadoEn = creadoEn;
    }

    public static PerfilDonante nuevo(UUID donanteId, String nombre, String email, String telefono, String whatsapp,
                                      MedioNotificacion medio, boolean personaHumana, Instant ahora) {
        return new PerfilDonante(donanteId, nombre, email, telefono, whatsapp, medio, personaHumana,
                CategoriaDonante.COLABORADOR, 0, true, ahora);
    }

    public void actualizarContacto(String nombre, String email, String telefono, String whatsapp, MedioNotificacion medio) {
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.whatsapp = whatsapp;
        this.medioPreferido = medio;
    }

    public Optional<Mision> misionActual(CatalogoMisiones catalogo) {
        return catalogo.en(categoria, indiceMision);
    }

    /**
     * Avanza a la siguiente misión. Si completó la categoría, asciende.
     *
     * @return la nueva categoría si hubo ascenso
     */
    Optional<CategoriaDonante> avanzar(CatalogoMisiones catalogo) {
        indiceMision++;
        if (indiceMision >= catalogo.de(categoria).size()) {
            Optional<CategoriaDonante> siguiente = categoria.siguiente();
            if (siguiente.isPresent()) {
                categoria = siguiente.get();
                indiceMision = 0;
                return siguiente;
            }
        }
        return Optional.empty();
    }

    public void configurarVisibilidad(boolean visibles) {
        this.insigniasVisibles = visibles;
    }

    public Destinatario comoDestinatario() {
        return new Destinatario(nombre, email, telefono, whatsapp, medioPreferido);
    }

    public UUID donanteId() { return donanteId; }
    public String nombre() { return nombre; }
    public String email() { return email; }
    public String telefono() { return telefono; }
    public String whatsapp() { return whatsapp; }
    public MedioNotificacion medioPreferido() { return medioPreferido; }
    public boolean personaHumana() { return personaHumana; }
    public CategoriaDonante categoria() { return categoria; }
    public int indiceMision() { return indiceMision; }
    public boolean insigniasVisibles() { return insigniasVisibles; }
    public Instant creadoEn() { return creadoEn; }
}
