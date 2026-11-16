package ar.utn.donatrack.front.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conversión de formularios HTML (campos planos) a los cuerpos JSON que esperan las APIs. */
public final class Formularios {

    private static final Pattern INDEXADO = Pattern.compile("^(\\w+)\\[(\\d+)]\\.(\\w+)$");

    private Formularios() {
    }

    public static String texto(Map<String, String> f, String campo) {
        String v = f.get(campo);
        return v == null || v.isBlank() ? null : v.trim();
    }

    public static Integer entero(Map<String, String> f, String campo) {
        String v = texto(f, campo);
        try {
            return v == null ? null : Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double decimal(Map<String, String> f, String campo) {
        String v = texto(f, campo);
        try {
            return v == null ? null : Double.valueOf(v.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Map<String, Object> direccion(Map<String, String> f) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("calle", texto(f, "calle"));
        d.put("altura", texto(f, "altura"));
        d.put("localidad", texto(f, "localidad"));
        d.put("provincia", texto(f, "provincia"));
        d.put("codigoPostal", texto(f, "codigoPostal"));
        d.put("latitud", decimal(f, "latitud"));
        d.put("longitud", decimal(f, "longitud"));
        return d;
    }

    public static Map<String, Object> contacto(Map<String, String> f) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("email", texto(f, "email"));
        c.put("telefono", texto(f, "telefono"));
        c.put("whatsapp", texto(f, "whatsapp"));
        c.put("medioPreferido", texto(f, "medioPreferido"));
        return c;
    }

    /** Agrupa campos "bienes[3].descripcion" en una lista ordenada de mapas. */
    public static List<Map<String, String>> filas(Map<String, String> f, String prefijo) {
        Map<Integer, Map<String, String>> porIndice = new TreeMap<>();
        f.forEach((clave, valor) -> {
            Matcher m = INDEXADO.matcher(clave);
            if (m.matches() && m.group(1).equals(prefijo)) {
                porIndice.computeIfAbsent(Integer.parseInt(m.group(2)), k -> new LinkedHashMap<>()).put(m.group(3), valor);
            }
        });
        return new ArrayList<>(porIndice.values());
    }
}
