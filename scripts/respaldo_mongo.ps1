# =====================================================================
#  Respaldo de la base "synergy" (Windows / PowerShell)
#  Uso:   .\scripts\respaldo_mongo.ps1
#  Restaurar: mongorestore --uri "mongodb://localhost:27017" --drop respaldos\mongo_AAAAMMDD_HHMM
#  Para hacerlo cada noche: Programador de tareas de Windows -> Crear tarea básica ->
#    Programa: powershell.exe  Argumentos: -File "RUTA\scripts\respaldo_mongo.ps1"
#  Requiere las MongoDB Database Tools (mongodump) instaladas.
# =====================================================================
param(
  [string]$Uri = "mongodb://localhost:27017",
  [string]$Base = "synergy",
  [int]$Conservar = 7
)
$carpeta = Join-Path $PSScriptRoot "..\respaldos"
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null
$destino = Join-Path $carpeta ("mongo_" + (Get-Date -Format "yyyyMMdd_HHmm"))
mongodump --uri $Uri --db $Base --gzip --out $destino
if ($LASTEXITCODE -ne 0) { Write-Error "mongodump falló"; exit 1 }
Write-Host "Respaldo guardado en $destino"
# Solo se guardan los últimos $Conservar respaldos
Get-ChildItem $carpeta -Directory -Filter "mongo_*" | Sort-Object Name -Descending |
  Select-Object -Skip $Conservar | Remove-Item -Recurse -Force
