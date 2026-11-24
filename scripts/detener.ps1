# Detiene el vigilante (primero, para que no reinicie lo que estamos apagando) y todos los componentes.
. (Join-Path $PSScriptRoot "comun.ps1")
if (-not (Test-Path $DirPids)) { Write-Host "No hay procesos registrados."; exit 0 }
Detener-Componente "vigilante"
Get-ChildItem $DirPids -Filter *.pid | ForEach-Object {
    $nombre = $_.BaseName
    Detener-Componente $nombre
    Write-Host "Detenido $nombre"
}
Write-Host "DonaTrack detenido."
