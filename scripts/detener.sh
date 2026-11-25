#!/usr/bin/env bash
# Detiene el vigilante (primero, para que no reinicie lo que estamos apagando) y todos los componentes.
# Recorre componentes.csv (no sólo los .pid): aunque falte o esté mal un .pid, verifica que cada
# puerto quede libre. Sale con 1 si alguno sigue ocupado.
source "$(dirname "$0")/comun.sh"
mkdir -p "$DIR_PIDS"
detener_componente vigilante

PENDIENTES=()
mapfile -t LISTA < <(componentes)
for linea in "${LISTA[@]}"; do
  read -r n _ puerto _ <<<"$linea"
  [[ -f "$DIR_PIDS/$n.pid" || -n "$(pid_en_puerto "$puerto")" ]] || continue
  if detener_componente "$n" "$puerto"; then echo "Detenido $n"; else PENDIENTES+=("$n:$puerto"); fi
done

if (( ${#PENDIENTES[@]} > 0 )); then
  echo "DonaTrack NO se detuvo por completo. Siguen ocupados: ${PENDIENTES[*]}"
  echo "Revisá qué proceso usa cada puerto (netstat -ano) antes de volver a iniciar."
  exit 1
fi
echo "DonaTrack detenido."
