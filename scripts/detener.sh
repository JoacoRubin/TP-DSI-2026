#!/usr/bin/env bash
# Detiene el vigilante (primero, para que no reinicie lo que estamos apagando) y todos los componentes.
source "$(dirname "$0")/comun.sh"
[[ -d "$DIR_PIDS" ]] || { echo "No hay procesos registrados."; exit 0; }
detener_componente vigilante
for f in "$DIR_PIDS"/*.pid; do
  [[ -e "$f" ]] || continue
  n="$(basename "$f" .pid)"
  detener_componente "$n" && echo "Detenido $n"
done
echo "DonaTrack detenido."
