# Compila la app y la deja lista para publicar con el backend.
#
#   .\publicar.ps1                 -> version web + APK
#   .\publicar.ps1 -SoloWeb        -> solo la version web
#
# La version web se copia al backend (src/main/resources/META-INF/resources),
# que la sirve en https://app-edu-financiera.onrender.com/zenfi/ . Despues hay
# que hacer commit y push del backend a develop para que Render la despliegue.
#
# No se copia la carpeta canvaskit (37 MB): la app la descarga del CDN de Google.

param(
    [string]$Backend = "$PSScriptRoot\..\..\App-Edu.-Financiera",
    [switch]$SoloWeb
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$destino = Join-Path $Backend "src\main\resources\META-INF\resources"
if (-not (Test-Path (Join-Path $Backend "pom.xml"))) {
    throw "No se encontro el backend en $Backend (usa -Backend <ruta>)"
}

Write-Host "Compilando la version web..."
flutter build web --release --base-href /zenfi/
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion web" }

if (Test-Path $destino) { Remove-Item -Recurse -Force $destino }
New-Item -ItemType Directory -Force $destino | Out-Null
Get-ChildItem build\web | Where-Object { $_.Name -ne "canvaskit" } |
    Copy-Item -Destination $destino -Recurse
Write-Host "Version web copiada a $destino"

if (-not $SoloWeb) {
    Write-Host "Compilando el APK..."
    flutter build apk --release
    if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion del APK" }
    Copy-Item build\app\outputs\flutter-apk\app-release.apk (Join-Path $destino "zenfi.apk")
    Write-Host "APK copiado: se descarga en https://app-edu-financiera.onrender.com/zenfi/zenfi.apk"
}
