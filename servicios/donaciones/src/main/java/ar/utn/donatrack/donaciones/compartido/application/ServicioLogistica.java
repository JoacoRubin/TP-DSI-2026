package ar.utn.donatrack.donaciones.compartido.application;

import java.util.UUID;

/** Puerto de salida hacia Logística (comandos sincrónicos que requieren confirmación). */
public interface ServicioLogistica {

    /**
     * Pide que la donación asignada se incluya en la próxima planificación de rutas. Lleva datos
     * desnormalizados (nombre de la entidad, dirección, descripción) para que Logística pueda
     * mostrar mapas y comprobantes sin consultar a Donaciones.
     */
    void solicitarEntrega(SolicitudEntrega solicitud);

    void cancelarEntrega(UUID donacionId);

    record SolicitudEntrega(UUID donacionId, UUID donanteId, String donanteNombre, UUID entidadId,
                            String entidadNombre, String direccion, double latitud, double longitud,
                            String descripcion, String subcategoria, int cantidad, String unidad,
                            double pesoKg, double volumenM3) {
    }
}
