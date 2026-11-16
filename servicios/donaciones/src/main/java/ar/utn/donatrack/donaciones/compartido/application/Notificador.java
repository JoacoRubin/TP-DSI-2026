package ar.utn.donatrack.donaciones.compartido.application;

import ar.utn.donatrack.contratos.notificaciones.Destinatario;

/** Puerto de salida: el adaptador lo publica en la cola del Servicio de Notificaciones. */
public interface Notificador {

    void notificar(Destinatario destinatario, String asunto, String mensaje, String tipo);
}
