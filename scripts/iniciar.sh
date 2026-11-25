#!/usr/bin/env bash
# =============================================================================
#  DonaTrack - arranque local (Linux / macOS / Git Bash en Windows)
#  Uso: ./scripts/iniciar.sh [--recompilar] [--sin-vigilante]
#  Componentes: scripts/componentes.csv (única fuente, compartida con iniciar.ps1 y el vigilante).
# =============================================================================
set -euo pipefail
source "$(dirname "$0")/comun.sh"
cd "$RAIZ"
mkdir -p logs

RECOMPILAR=0; VIGILANTE=1
for a in "$@"; do
  case "$a" in --recompilar) RECOMPILAR=1 ;; --sin-vigilante) VIGILANTE=0 ;; esac
done

mapfile -t LISTA < <(componentes)
for linea in "${LISTA[@]}"; do read -r _ jar _ _ <<<"$linea"; [[ -f "$jar" ]] || RECOMPILAR=1; done
if (( RECOMPILAR )); then
  echo "Compilando todos los módulos..."
  # clean: sin él, maven-jar-plugin NO rearma el jar de un módulo cuyo código no cambió, aunque haya
  # cambiado una dependencia interna (p. ej. plataforma/observabilidad), y se ejecuta la versión vieja.
  ./mvnw -q -B -DskipTests clean package
fi

# Trazas distribuidas: sólo se exportan si el stack de observabilidad (Tempo) está levantado.
if salud_ok "http://localhost:3200/ready" 2; then
  export DONATRACK_TRAZAS=true
  echo "Stack de observabilidad detectado: trazas activadas (Grafana en http://localhost:3000)"
fi

CAIDOS=()
for linea in "${LISTA[@]}"; do
  read -r n jar puerto espera <<<"$linea"
  echo "Iniciando $n..."
  iniciar_componente "$n" "$jar" >/dev/null
  # broker y autenticación primero: el resto depende de ellos al arrancar.
  if [[ "$n" == "broker" || "$n" == "autenticacion" ]]; then
    if esperar_salud "$puerto" "$espera"; then confirmar_pid "$n" "$puerto"; echo "  $n OK"; else echo "  $n SIN RESPUESTA"; CAIDOS+=("$n"); fi
  fi
done
for linea in "${LISTA[@]:2}"; do
  read -r n _ puerto espera <<<"$linea"
  if esperar_salud "$puerto" "$espera"; then confirmar_pid "$n" "$puerto"; echo "  $n OK"; else echo "  $n SIN RESPUESTA"; CAIDOS+=("$n"); fi
done

if (( VIGILANTE )); then
  nohup "$RAIZ/scripts/vigilante.sh" > logs/vigilante.log 2>&1 &
  # El PID lo registra el propio vigilante ($$): el $! de acá es la etapa nohup, que en MSYS
  # se reemplaza por otro proceso al hacer exec y deja un PID que ya no existe.
  echo "Vigilante activo (reinicio automático). Eventos en logs/vigilante.json"
fi

echo
if (( ${#CAIDOS[@]} > 0 )); then
  echo "DonaTrack arrancó INCOMPLETO. Sin respuesta: ${CAIDOS[*]}"
  echo "Revisá logs/<componente>.log. Para detener: ./scripts/detener.sh"
  exit 1
fi
echo "DonaTrack corriendo en http://localhost:8080  (cuentas demo con clave Demo1234!)"
echo "Para detener: ./scripts/detener.sh"
