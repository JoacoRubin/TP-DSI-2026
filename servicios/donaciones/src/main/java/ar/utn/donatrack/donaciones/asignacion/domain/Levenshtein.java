package ar.utn.donatrack.donaciones.asignacion.domain;

import java.text.Normalizer;

/** Distancia de edición de Levenshtein (programación dinámica, O(n·m) tiempo, O(m) memoria). */
public final class Levenshtein {

    private Levenshtein() {
    }

    public static int distancia(String a, String b) {
        int[] previa = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previa[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, previa[j] + 1), previa[j - 1] + costo);
            }
            int[] tmp = previa;
            previa = actual;
            actual = tmp;
        }
        return previa[b.length()];
    }

    /** v1 = 1 − lev(a, b) / max(|a|, |b|), comparando sin mayúsculas ni acentos. */
    public static double similitud(String a, String b) {
        String x = normalizar(a);
        String y = normalizar(b);
        int max = Math.max(x.length(), y.length());
        if (max == 0) {
            return 1.0;
        }
        return 1.0 - (double) distancia(x, y) / max;
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase().trim().replaceAll("\\s+", " ");
    }
}
