package ar.utn.donatrack.donaciones.importacion.domain;

import ar.utn.donatrack.contratos.errores.ReglaDeNegocioException;
import ar.utn.donatrack.donaciones.donantes.domain.MediosContacto;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDocumento;
import ar.utn.donatrack.donaciones.donantes.domain.TipoDonante;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formato del archivo histórico: TipoPersona, TipoDoc, Documento, Nombre/Razón Social, Email, Teléfono.
 * Soporta separador "," o ";" (detectado en el encabezado) y campos entre comillas (RFC 4180).
 */
public final class FormatoCsvDonantes {

    private static final int COLUMNAS = 6;
    private final char separador;

    private FormatoCsvDonantes(char separador) {
        this.separador = separador;
    }

    public static FormatoCsvDonantes desdeEncabezado(String encabezado) {
        if (encabezado == null || encabezado.isBlank()) {
            throw new ReglaDeNegocioException("El archivo está vacío");
        }
        String limpio = encabezado.replace("﻿", "");
        char sep = limpio.indexOf(';') >= 0 && limpio.indexOf(',') < 0 ? ';' : ',';
        if (!limpio.toLowerCase(Locale.ROOT).startsWith("tipopersona")) {
            throw new ReglaDeNegocioException("Encabezado inválido: se esperaba 'TipoPersona" + sep + "TipoDoc" + sep + "...'");
        }
        return new FormatoCsvDonantes(sep);
    }

    public FilaDonante parsear(String linea, int numeroLinea) {
        List<String> campos = dividir(linea);
        if (campos.size() < COLUMNAS - 1) {
            throw error(numeroLinea, "se esperaban " + COLUMNAS + " columnas y hay " + campos.size());
        }
        try {
            TipoDonante tipo = TipoDonante.valueOf(campos.get(0).trim().toUpperCase(Locale.ROOT));
            TipoDocumento tipoDoc = TipoDocumento.valueOf(campos.get(1).trim().toUpperCase(Locale.ROOT));
            String nombre = campos.get(3).trim();
            if (nombre.isEmpty()) {
                throw error(numeroLinea, "falta el nombre o razón social");
            }
            String email = MediosContacto.validarEmail(campos.get(4));
            String telefono = campos.size() > 5 && !campos.get(5).isBlank() ? campos.get(5).trim() : null;
            return new FilaDonante(numeroLinea, tipo, tipoDoc, campos.get(2).trim(), nombre, email, telefono);
        } catch (IllegalArgumentException e) {
            throw error(numeroLinea, "valor no reconocido (" + e.getMessage() + ")");
        } catch (ReglaDeNegocioException e) {
            if (e.getMessage().startsWith("Línea")) {
                throw e;
            }
            throw error(numeroLinea, e.getMessage());
        }
    }

    private static ReglaDeNegocioException error(int linea, String detalle) {
        return new ReglaDeNegocioException("Línea " + linea + ": " + detalle);
    }

    List<String> dividir(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (entreComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else {
                    entreComillas = !entreComillas;
                }
            } else if (c == separador && !entreComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
