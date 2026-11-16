#!/usr/bin/env bash
# =============================================================================
#  DonaTrack - arranque local (Linux / macOS / Git Bash en Windows)
#  Uso: ./scripts/iniciar.sh [--recompilar]
# =============================================================================
set -euo pipefail
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RAIZ"
mkdir -p logs
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

COMPONENTES=(
  "broker|infraestructura-local/broker/target/broker-1.0.0-SNAPSHOT.jar|8161"
  "autenticacion|servicios/autenticacion/target/servicio-autenticacion-1.0.0-SNAPSHOT.jar|8085"
  "notificaciones|servicios/notificaciones/target/servicio-notificaciones-1.0.0-SNAPSHOT.jar|8084"
  "donaciones|servicios/donaciones/target/servicio-donaciones-1.0.0-SNAPSHOT.jar|8081"
  "logistica|servicios/logistica/target/servicio-logistica-1.0.0-SNAPSHOT.jar|8082"
  "incentivos|servicios/incentivos/target/servicio-incentivos-1.0.0-SNAPSHOT.jar|8083"
  "planificador|externos/planificador-rutas/target/planificador-rutas-1.0.0-SNAPSHOT.jar|8090"
  "front|front/target/front-1.0.0-SNAPSHOT.jar|8080"
)

falta=0
for c in "${COMPONENTES[@]}"; do IFS='|' read -r _ jar _ <<<"$c"; [[ -f "$jar" ]] || falta=1; done
if [[ "${1:-}" == "--recompilar" || $falta -eq 1 ]]; then
  echo "Compilando todos los módulos..."
  ./mvnw -q -B -DskipTests package
fi

esperar() { # url nombre segundos
  local limite=$((SECONDS + $3))
  while (( SECONDS < limite )); do
    curl -fs "$1" >/dev/null 2>&1 && { echo "  $2 OK"; return 0; }
    sleep 2
  done
  echo "  $2 no respondió en $3 s (ver logs/$2.log)"; return 1
}

: > logs/pids.txt
for c in "${COMPONENTES[@]}"; do
  IFS='|' read -r nombre jar puerto <<<"$c"
  echo "Iniciando $nombre..."
  nohup "$JAVA" -Xmx384m -XX:+UseSerialGC -jar "$jar" > "logs/$nombre.log" 2>&1 &
  echo $! >> logs/pids.txt
  if [[ "$nombre" == "broker" || "$nombre" == "autenticacion" ]]; then
    esperar "http://localhost:$puerto/actuator/health" "$nombre" 120 || true
  fi
done
for c in "${COMPONENTES[@]:2}"; do
  IFS='|' read -r nombre _ puerto <<<"$c"
  segundos=120; [[ "$nombre" == "logistica" ]] && segundos=240
  esperar "http://localhost:$puerto/actuator/health" "$nombre" $segundos || true
done
echo
echo "DonaTrack corriendo en http://localhost:8080  (cuentas demo con clave Demo1234!)"
echo "Para detener: ./scripts/detener.sh"
