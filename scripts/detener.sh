#!/usr/bin/env bash
RAIZ="$(cd "$(dirname "$0")/.." && pwd)"
[[ -f "$RAIZ/logs/pids.txt" ]] || { echo "No hay procesos registrados."; exit 0; }
while read -r pid; do kill "$pid" 2>/dev/null && echo "Detenido $pid"; done < "$RAIZ/logs/pids.txt"
rm -f "$RAIZ/logs/pids.txt"
echo "DonaTrack detenido."
