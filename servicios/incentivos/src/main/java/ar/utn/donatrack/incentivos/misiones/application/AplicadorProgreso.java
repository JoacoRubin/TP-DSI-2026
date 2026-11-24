package ar.utn.donatrack.incentivos.misiones.application;

import ar.utn.donatrack.incentivos.actividad.domain.ActividadRepositorio;
import ar.utn.donatrack.incentivos.actividad.domain.HistorialDonante;
import ar.utn.donatrack.incentivos.misiones.domain.CategoriaDonante;
import ar.utn.donatrack.incentivos.misiones.domain.InsigniaObtenida;
import ar.utn.donatrack.incentivos.misiones.domain.MotorIncentivos;
import ar.utn.donatrack.incentivos.misiones.domain.PerfilDonante;
import ar.utn.donatrack.incentivos.misiones.domain.Repositorios;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;

/**
 * Evalúa el progreso del donante y aplica los efectos: insignias, ascensos, notificaciones y
 * difusión en redes. Compartido por los casos de uso que reaccionan a eventos de Donaciones.
 */
public class AplicadorProgreso {

    private final MotorIncentivos motor;
    private final ActividadRepositorio actividades;
    private final Repositorios.PerfilRepositorio perfiles;
    private final Repositorios.InsigniaRepositorio insignias;
    private final Puertos.Notificador notificador;
    private final Puertos.DifusorLogros difusor;
    private final Clock reloj;

    public AplicadorProgreso(MotorIncentivos motor, ActividadRepositorio actividades,
                             Repositorios.PerfilRepositorio perfiles, Repositorios.InsigniaRepositorio insignias,
                             Puertos.Notificador notificador, Puertos.DifusorLogros difusor, Clock reloj) {
        this.motor = motor;
        this.actividades = actividades;
        this.perfiles = perfiles;
        this.insignias = insignias;
        this.notificador = notificador;
        this.difusor = difusor;
        this.reloj = reloj;
    }

    /**
     * El hecho, sin formato de red social: el emoji, la @mención y los hashtags los agrega el flujo de
     * difusión (n8n), que es lo que cambia si se publica en otra red. Antes se duplicaban.
     */
    static String textoDifusion(String nombre, String insignia) {
        return nombre + " obtuvo la insignia \"" + insignia + "\" en DonaTrack. ¡Gracias por ayudar a quienes más lo necesitan!";
    }

    public MotorIncentivos.Resultado aplicar(PerfilDonante perfil) {
        Instant ahora = reloj.instant();
        HistorialDonante historial = new HistorialDonante(actividades.deDonante(perfil.donanteId()), reloj.getZone());
        MotorIncentivos.Resultado r = motor.evaluar(perfil, historial, YearMonth.now(reloj), ahora);
        perfiles.guardar(perfil);
        if (!r.huboCambios()) {
            return r;
        }
        insignias.guardarTodas(r.insignias());
        for (InsigniaObtenida i : r.insignias()) {
            String texto = textoDifusion(perfil.nombre(), i.nombre());
            notificador.notificar(perfil.comoDestinatario(), "¡Misión cumplida! " + i.icono(),
                    "Completaste la misión \"" + i.nombre() + "\" y ganaste una nueva insignia. ¡Seguí así!", "MISION_CUMPLIDA");
            if (perfil.insigniasVisibles()) {
                difusor.difundir(perfil, i, texto);
            }
        }
        for (CategoriaDonante c : r.ascensos()) {
            notificador.notificar(perfil.comoDestinatario(), "¡Subiste de categoría!",
                    "Ahora sos " + c.etiqueta() + " en DonaTrack. Tu compromiso transforma realidades.", "CATEGORIA");
        }
        return r;
    }
}
