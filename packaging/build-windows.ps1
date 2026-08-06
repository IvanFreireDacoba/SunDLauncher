# Empaqueta SunDLauncher como un instalador .exe nativo de Windows, con un
# runtime de Java 17 completo embebido: el usuario final no necesita tener
# Java instalado. Doble click en el .exe -> instala la app (acceso directo
# en el menu de inicio, entrada en "Agregar o quitar programas", etc.).
#
# Requiere: JDK 17+ (con jpackage) y el WiX Toolset v3 (choco install
# wixtoolset). Los runners windows-latest de GitHub Actions ya traen ambos.
#
# La version se toma de <version> en pom.xml.
#
# Uso:
#   pwsh packaging/build-windows.ps1
#
# Salida: target/dist/SunDLauncher-<version>-windows-x64.zip
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

# UUID fijo para que jpackage/WiX traten las versiones sucesivas como
# actualizaciones del mismo producto en vez de instalaciones duplicadas.
$UPGRADE_UUID = "6ef41ac1-0103-4e8f-a460-f7efd74de510"

if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    Write-Error "No se encuentra 'jpackage' en el PATH. Necesitas un JDK 17+ y JAVA_HOME configurado."
    exit 1
}
if (-not $env:JAVA_HOME) {
    Write-Error "Define JAVA_HOME apuntando a tu JDK 17."
    exit 1
}

Write-Host "==> leyendo version de pom.xml"
$VERSION = (mvn -q help:evaluate "-Dexpression=project.version" -DforceStdout).Trim()
Write-Host "    version: $VERSION"

Write-Host "==> mvn clean package"
mvn -q clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> jpackage (instalador .exe, runtime completo embebido)"
Remove-Item -Recurse -Force target\dist -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force target\jpackage-input -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path target\jpackage-input | Out-Null
Copy-Item target\SunDLauncher.jar target\jpackage-input\

# Resolver symlinks/junctions de JAVA_HOME antes de pasarlo a --runtime-image:
# jpackage copia el runtime con Files.walkFileTree sin seguir symlinks, y si
# la raiz es un symlink lo trata como fichero en vez de directorio (visto en
# CI Linux con el hostedtoolcache de GitHub Actions; fix defensivo tambien
# aqui por si el runner de Windows tiene el mismo patron).
$RUNTIME_IMAGE = (Resolve-Path $env:JAVA_HOME).Path

jpackage `
  --type exe `
  --name SunDLauncher `
  --input target\jpackage-input `
  --main-jar SunDLauncher.jar `
  --main-class es.sund.launcher.Main `
  --icon packaging\icons\SunDLauncher.ico `
  --app-version $VERSION `
  --vendor "SunDStudios" `
  --win-shortcut `
  --win-menu `
  --win-dir-chooser `
  --win-upgrade-uuid $UPGRADE_UUID `
  --runtime-image $RUNTIME_IMAGE `
  --dest target\dist
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "==> comprimiendo en zip para repartir"
$zipName = "SunDLauncher-$VERSION-windows-x64.zip"
$exePath = "target\dist\SunDLauncher-$VERSION.exe"
Compress-Archive -Path $exePath -DestinationPath "target\dist\$zipName" -Force

Write-Host ""
Write-Host "Listo: target\dist\$zipName"
Write-Host "  El jugador lo descomprime y hace doble click en el .exe: instala"
Write-Host "  la app (sin JDK, con acceso directo y desinstalador)."
