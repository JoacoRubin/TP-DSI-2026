#!/usr/bin/env bash
# Funciones compartidas por iniciar.sh, vigilante.sh y detener.sh (se cargan con "source").
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIR_PIDS="$RAIZ/logs/pids"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"

# Emite "nombre jar puerto espera" por línea, desde scripts/componentes.csv (única fuente).
componentes() {
  tail -n +2 "$RAIZ/scripts/componentes.csv" | tr -d '\r' | while IFS=, read -r n j p e; do
    [[ -n "$n" ]] && echo "$n $j $p $e"
  done
}

salud_ok() { curl -fs --max-time "${2:-3}" "$1" >/dev/null 2>&1; }

esperar_salud() { # puerto segundos
  local limite=$((SECONDS + $2))
  while (( SECONDS < limite )); do
    salud_ok "http://localhost:$1/actuator/health" 2 && return 0
    sleep 2
  done
  return 1
}

pid_de() { [[ -f "$DIR_PIDS/$1.pid" ]] && cat "$DIR_PIDS/$1.pid"; }

# Git Bash / Cygwin en Windows: $! es un PID de MSYS, NO el del java.exe real, y `kill` no lo
# alcanza. Por eso ahí se registra el PID de Windows (/proc/<pid>/winpid) y se usa tasklist/taskkill.
es_windows() { [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* ]]; }

proceso_vivo() {
  [[ -n "${1:-}" ]] || return 1
  if es_windows; then tasklist //FI "PID eq $1" //NH 2>/dev/null | grep -qw "$1"; else kill -0 "$1" 2>/dev/null; fi
}

# PID que hay que registrar para un proceso lanzado en segundo plano ($!). En Windows TODOS los
# PID registrados (java.exe y también el vigilante) deben ser de Windows: detener_componente usa
# tasklist/taskkill, y con un PID de MSYS no encuentra el proceso o, peor, mata otro que coincida.
pid_windows() { # pid-de-$!
  local pid="$1"
  if es_windows; then
    for _ in 1 2 3 4 5; do [[ -r "/proc/$pid/winpid" ]] && break; sleep 0.2; done
    [[ -r "/proc/$pid/winpid" ]] && pid="$(cat "/proc/$pid/winpid")"
  fi
  echo "$pid"
}

iniciar_componente() { # nombre jar
  mkdir -p "$DIR_PIDS"
  # Conservar el log anterior: si se reinició por una caída, ahí está la causa.
  [[ -f "$RAIZ/logs/$1.log" ]] && mv -f "$RAIZ/logs/$1.log" "$RAIZ/logs/$1.anterior.log"
  local pid
  pid=$(cd "$RAIZ" && { nohup "$JAVA" -Xmx384m -XX:+UseSerialGC -jar "$2" > "logs/$1.log" 2>&1 & echo $!; })
  pid="$(pid_windows "$pid")"
  echo "$pid" > "$DIR_PIDS/$1.pid"
  echo "$pid"
}

detener_componente() { # nombre
  local id; id="$(pid_de "$1")"
  if proceso_vivo "$id"; then
    if es_windows; then taskkill //PID "$id" //T //F >/dev/null 2>&1; else kill "$id" 2>/dev/null; fi
  fi
  rm -f "$DIR_PIDS/$1.pid"
}

# Log en JSON (mismo formato ECS que los servicios) para que Alloy/Loki lo muestren en Grafana.
log_json() { # servicio nivel mensaje
  local msg=${3//\\/\\\\}; msg=${msg//\"/\\\"}
  printf '{"@timestamp":"%s","log":{"level":"%s"},"service":{"name":"%s"},"message":"%s"}\n' \
    "$(date -u +%Y-%m-%dT%H:%M:%S.000Z)" "$2" "$1" "$msg" >> "$RAIZ/logs/$1.json"
  echo "[$2] $3"
}
