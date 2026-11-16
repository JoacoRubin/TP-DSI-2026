# Detiene todos los procesos lanzados por iniciar.ps1
$raiz = Split-Path -Parent $PSScriptRoot
$archivo = Join-Path $raiz "logs\pids.txt"
if (-not (Test-Path $archivo)) { Write-Host "No hay procesos registrados."; exit 0 }
Get-Content $archivo | ForEach-Object {
    try { Stop-Process -Id ([int]$_) -Force -ErrorAction Stop; Write-Host "Detenido PID $_" } catch { }
}
Remove-Item $archivo
Write-Host "DonaTrack detenido."
