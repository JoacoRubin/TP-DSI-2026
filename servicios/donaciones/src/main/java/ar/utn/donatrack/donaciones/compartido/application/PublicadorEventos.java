package ar.utn.donatrack.donaciones.compartido.application;

import ar.utn.donatrack.contratos.eventos.EventosDonaciones;

/** Puerto de salida: eventos de dominio hacia otros servicios (Incentivos). */
public interface PublicadorEventos {

    void donacionRegistrada(EventosDonaciones.DonacionRegistrada evento);

    void donacionEntregada(EventosDonaciones.DonacionEntregada evento);
}
