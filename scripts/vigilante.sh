#!/usr/bin/env bash
# =============================================================================
#  DonaTrack - vigilante: monitoreo proactivo con reinicio automático (Entrega 6)
#  Misma política que vigilante.ps1 (ver ese archivo). Lo lanza iniciar.sh en segundo plano.
#  Variables: INTERVALO=15 FALLOS_PARA_REINICIAR=3 MAX_REINICIOS=5 VENTANA_MIN=10
# =============================================================================
set -uo pipefail
source "$(dirname "$0")/comun.sh"

INTERVALO=${INTERVALO:-15}
FALLOS_PARA_REINICIAR=${FALLOS_PARA_REINICIAR:-3}
MAX_REINICIOS=${MAX_REINICIOS:-5}
VENTANA_MIN=${VENTANA_MIN:-10}

mkdir -p "$DIR_PIDS"
pid_windows $$ > "$DIR_PIDS/vigilante.pid" # lo usa detener.sh (y apagando(), más abajo)

declare -A FALLOS GRACIA REINICIOS ABANDONADO
mapfile -t LISTA < <(componentes)
ahora() { date +%s; }
for linea in "${LISTA[@]}"; do
  read -r n _ _ e <<<"$linea"
  FALLOS[$n]=0; GRACIA[$n]=$(( $(ahora) + e )); REINICIOS[$n]=""; ABANDONADO[$n]=0
done
log_json vigilante INFO "Vigilante iniciado: cada ${INTERVALO}s, reinicio tras ${FALLOS_PARA_REINICIAR} fallos, máx. ${MAX_REINICIOS} reinicios en ${VENTANA_MIN} min"

# detener.sh borra logs/pids/vigilante.pid: si ya no está, se está apagando DonaTrack y NO hay
# que relanzar nada (segunda barrera, por si no se pudo matar este proceso).
apagando() { [[ ! -f "$DIR_PIDS/vigilante.pid" ]]; }

while true; do
  for linea in "${LISTA[@]}"; do
    apagando && { log_json vigilante INFO "Vigilante detenido (DonaTrack se está apagando)"; exit 0; }
    read -r n jar puerto espera <<<"$linea"
    (( ABANDONADO[$n] == 1 || $(ahora) < GRACIA[$n] )) && continue

    vivo=0; proceso_vivo "$(pid_de "$n")" && vivo=1
    # Responde pero el PID registrado no está vivo: el registrado estaba mal, no hay que reiniciar
    # (se relanzaría encima del que ya escucha). confirmar_pid sólo lo corrige si el PID es nuestro.
    if (( ! vivo )) && salud_ok "http://localhost:$puerto/actuator/health"; then
      confirmar_pid "$n" "$puerto"; proceso_vivo "$(pid_de "$n")" && vivo=1
    fi
    if (( vivo )) && salud_ok "http://localhost:$puerto/actuator/health"; then
      (( FALLOS[$n] > 0 )) && log_json vigilante INFO "$n se recuperó sin reiniciar"
      FALLOS[$n]=0; continue
    fi

    FALLOS[$n]=$(( FALLOS[$n] + 1 ))
    if (( vivo && FALLOS[$n] < FALLOS_PARA_REINICIAR )); then
      log_json vigilante WARN "$n no responde al health check (${FALLOS[$n]}/${FALLOS_PARA_REINICIAR})"; continue
    fi

    desde=$(( $(ahora) - VENTANA_MIN * 60 )); recientes=""
    for t in ${REINICIOS[$n]}; do (( t > desde )) && recientes+="$t "; done
    REINICIOS[$n]="$recientes"
    if (( $(wc -w <<<"$recientes") >= MAX_REINICIOS )); then
      ABANDONADO[$n]=1
      log_json vigilante ERROR "$n se reinició ${MAX_REINICIOS} veces en ${VENTANA_MIN} min: NO se reintenta más. Revisá logs/$n.anterior.log"
      continue
    fi

    apagando && { log_json vigilante INFO "Vigilante detenido (DonaTrack se está apagando)"; exit 0; }
    motivo="el proceso terminó"; (( vivo )) && motivo="health check fallido ${FALLOS[$n]} veces"
    log_json vigilante WARN "Reiniciando $n ($motivo)"
    if ! detener_componente "$n" "$puerto" 2>/dev/null; then
      log_json vigilante ERROR "$n: el puerto $puerto sigue ocupado por otro proceso; no se relanza"
      continue
    fi
    nuevo=$(iniciar_componente "$n" "$jar")
    REINICIOS[$n]+="$(ahora) "; FALLOS[$n]=0; GRACIA[$n]=$(( $(ahora) + espera ))
    log_json vigilante INFO "$n relanzado (PID $nuevo)"
  done
  sleep "$INTERVALO"
done
