package ar.utn.donatrack.front.web;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * Helper de presentación disponible en las vistas como {@code @fmt}. Los datos llegan como JSON
 * (fechas ISO-8601 en texto): acá se formatean para humanos en zona horaria argentina.
 */
@Component("fmt")
public class Formato {

    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-AR"));

    private static final Map<String, String> CLASE_ESTADO = Map.ofEntries(
            Map.entry("EN_DEPOSITO", "neutro"), Map.entry("ASIGNADA", "info"), Map.entry("LISTA_PARA_ENTREGAR", "info"),
            Map.entry("EN_TRASLADO", "aviso"), Map.entry("ENTREGADA", "exito"), Map.entry("ENTREGA_FALLIDA", "error"),
            Map.entry("VENCIDA", "error"), Map.entry("PENDIENTE", "neutro"), Map.entry("EN_PLANIFICACION", "info"),
            Map.entry("PLANIFICADA", "info"), Map.entry("NO_RECIBIDA", "error"), Map.entry("CANCELADA", "neutro"),
            Map.entry("EN_CURSO", "aviso"), Map.entry("FINALIZADA", "exito"), Map.entry("ENVIADA", "exito"),
            Map.entry("FALLIDA", "error"), Map.entry("COMPLETADA", "exito"), Map.entry("BLOQUEADA", "neutro"),
            Map.entry("FINALIZADA_IMPORT", "exito"));

    private static final Map<String, String> ETIQUETA_ESTADO = Map.ofEntries(
            Map.entry("EN_DEPOSITO", "En depósito"), Map.entry("ASIGNADA", "Asignación realizada"),
            Map.entry("LISTA_PARA_ENTREGAR", "Lista para entregar"), Map.entry("EN_TRASLADO", "En traslado"),
            Map.entry("ENTREGADA", "Entregada"), Map.entry("ENTREGA_FALLIDA", "Entrega fallida"),
            Map.entry("VENCIDA", "Vencida"), Map.entry("PENDIENTE", "Pendiente"),
            Map.entry("EN_PLANIFICACION", "En planificación"), Map.entry("PLANIFICADA", "Planificada"),
            Map.entry("NO_RECIBIDA", "No recibida"), Map.entry("CANCELADA", "Cancelada"),
            Map.entry("EN_CURSO", "En curso"), Map.entry("FINALIZADA", "Finalizada"),
            Map.entry("COMPLETADA", "Completada"), Map.entry("BLOQUEADA", "Bloqueada"));

    public String fecha(Object iso) {
        if (iso == null) {
            return "—";
        }
        String s = iso.toString();
        try {
            return s.length() == 10 ? LocalDate.parse(s).format(FECHA) : Instant.parse(s).atZone(ZONA).format(FECHA);
        } catch (Exception e) {
            return s;
        }
    }

    public String fechaHora(Object iso) {
        if (iso == null) {
            return "—";
        }
        try {
            return Instant.parse(iso.toString()).atZone(ZONA).format(FECHA_HORA);
        } catch (Exception e) {
            return iso.toString();
        }
    }

    public String mes(Object yyyyMM) {
        try {
            return java.time.YearMonth.parse(yyyyMM.toString()).atDay(1).format(MES);
        } catch (Exception e) {
            return String.valueOf(yyyyMM);
        }
    }

    public String claseEstado(Object estado) {
        return CLASE_ESTADO.getOrDefault(String.valueOf(estado), "neutro");
    }

    public String estado(Object estado) {
        return ETIQUETA_ESTADO.getOrDefault(String.valueOf(estado), String.valueOf(estado));
    }

    public String porcentaje(Object valor) {
        if (valor == null) {
            return "—";
        }
        double v = Double.parseDouble(valor.toString());
        return Math.round(v * 100) + "%";
    }

    public int max(Collection<Map<String, Object>> filas, String campo) {
        return filas.stream().mapToInt(f -> f.get(campo) == null ? 0 : ((Number) f.get(campo)).intValue()).max().orElse(0);
    }
}
