package ar.utn.donatrack.donaciones.donaciones.application;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;
import ar.utn.donatrack.donaciones.donantes.domain.Donante;
import ar.utn.donatrack.donaciones.donantes.domain.PersonaHumana;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDonante;

final class ResumenDonante {

    private ResumenDonante() {
    }

    static EventosDonaciones.DonanteResumen de(Donante d) {
        boolean aceptaDifusion = d instanceof PersonaHumana h && h.aceptaDifusionLogros();
        return new EventosDonaciones.DonanteResumen(d.id(), d.nombreVisible(), d.email(), d.contacto().telefono(),
                d.contacto().whatsapp(), d.contacto().preferido(), d.tipo() == TipoDonante.HUMANA, aceptaDifusion);
    }
}
