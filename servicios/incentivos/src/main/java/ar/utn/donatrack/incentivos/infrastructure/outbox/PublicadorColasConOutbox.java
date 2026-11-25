package ar.utn.donatrack.incentivos.infrastructure.outbox;

import ar.utn.donatrack.contratos.TiposMensaje;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Publicación JMS confiable (avisos de misión cumplida y de categoría): en vez de enviar al broker,
 * registra el mensaje en la bandeja de salida dentro de la transacción del caso de uso. Si hay
 * rollback, el mensaje desaparece con ella; si confirma, {@link DespachadorOutbox} lo publica
 * aunque el proceso se caiga justo después del commit (cosa que la publicación post-commit perdía).
 * <p>
 * Se guarda como {@code JMS:<tipo lógico>} (el mismo identificador de {@link TiposMensaje} que
 * viaja en el mensaje) y la cola como clave: los mensajes de una cola salen en el orden en que
 * se registraron.
 */
public class PublicadorColasConOutbox {

    static final String PREFIJO = "JMS:";

    private static final Map<Class<?>, String> TIPOS = TiposMensaje.TODOS.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    private final BandejaSalida bandeja;
    private final ObjectMapper json;
    private final Clock reloj;

    public PublicadorColasConOutbox(BandejaSalida bandeja, ObjectMapper json, Clock reloj) {
        this.bandeja = bandeja;
        this.json = json;
        this.reloj = reloj;
    }

    public void publicar(String cola, Object mensaje) {
        String tipo = TIPOS.get(mensaje.getClass());
        if (tipo == null) {
            // Falla en el caso de uso (y lo revierte) en lugar de dejar un mensaje que nunca podría salir.
            throw new IllegalArgumentException("Sin tipo lógico en TiposMensaje: " + mensaje.getClass().getName());
        }
        try {
            bandeja.guardar(MensajeSaliente.nuevo(PREFIJO + tipo, cola, json.writeValueAsString(mensaje), reloj.instant()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el mensaje " + tipo, e);
        }
    }

    /** Clase registrada para un tipo guardado, o null si no es un mensaje JMS (o ya no existe). */
    static Class<?> claseDe(String tipoGuardado) {
        return tipoGuardado.startsWith(PREFIJO) ? TiposMensaje.TODOS.get(tipoGuardado.substring(PREFIJO.length())) : null;
    }
}
