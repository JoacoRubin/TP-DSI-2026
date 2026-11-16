#!/usr/bin/env bash
# Genera un CSV de prueba de N donantes (por defecto 20.000) con el formato del enunciado.
# Incluye ~1% de filas inválidas a propósito, para verificar el reporte de errores por línea.
# Uso: ./scripts/generar-csv-donantes.sh [filas] [archivo]
FILAS=${1:-20000}
SALIDA=${2:-docs/datos/donantes-${FILAS}.csv}
mkdir -p "$(dirname "$SALIDA")"
awk -v n="$FILAS" 'BEGIN {
  srand(2026);
  split("Ana,Juan,María,Lucas,Sofía,Mateo,Valentina,Martín,Camila,Diego,Lucía,Tomás", nom, ",");
  split("Pérez,Gómez,Fernández,López,Díaz,Martínez,Rodríguez,Sosa,Romero,Álvarez", ape, ",");
  split("Panificadora,Logística,Textil,Metalúrgica,Farmacia,Constructora,Librería,Distribuidora", rub, ",");
  print "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono";
  for (i = 1; i <= n; i++) {
    if (i % 100 == 0) { print "HUMANA,DNI,123,Fila Inválida " i ",sin-arroba-" i ","; continue }
    if (i % 7 == 0) {
      printf "JURIDICA,CUIT,30-%08d-%d,\"%s %d S.A.\",empresa%d@donantes.test,+54 11 4%03d-%04d\n", 10000000 + i, i % 10, rub[1 + i % 8], i, i, i % 1000, i % 10000
    } else {
      printf "HUMANA,DNI,%d,%s %s,persona%d@donantes.test,+54 9 11 5%03d-%04d\n", 20000000 + i, nom[1 + i % 12], ape[1 + i % 10], i, i % 1000, i % 10000
    }
  }
}' > "$SALIDA"
echo "Generado $SALIDA ($(wc -l < "$SALIDA") líneas)"
