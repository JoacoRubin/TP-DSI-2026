package ar.utn.donatrack.logistica.compartido.application;

import ar.utn.donatrack.contratos.eventos.EventosLogistica;

/** Puerto de salida: hechos logísticos que interesan a Donaciones. */
public interface PublicadorEventos {

    void publicar(EventosLogistica.EntregaPlanificada evento);

    void publicar(EventosLogistica.RutaIniciada evento);

    void publicar(EventosLogistica.EntregaRealizada evento);

    void publicar(EventosLogistica.EntregaNoSatisfactoria evento);

    void publicar(EventosLogistica.EntregaReprogramada evento);
}
