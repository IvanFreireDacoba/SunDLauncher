# Empaqueta SunDLauncher como un instalador .exe nativo de Windows, con un
# runtime de Java 21 completo embebido: el usuario final no necesita tener
# Java instalado. Doble click en el .exe -> instala la app (acceso directo
# en el menu de inicio, entrada en "Agregar o quitar programas", etc.).
#
# Requiere: JDK 21+ (con jpackage) y el WiX Toolset v3 (choco install
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
    Write-Error "No se encuentra 'jpackage' en el PATH. Necesitas un JDK 21+ y JAVA_HOME configurado."
    exit 1
}
if (-not $env:JAVA_HOME) {
    Write-Error "Define JAVA_HOME apuntando a tu JDK 21."
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

$exePath = "target\dist\SunDLauncher-$VERSION.exe"

# ---- Firma de código (Microsoft Trusted Signing) ----
# Opcional y silenciosa: si no están las 6 variables de entorno de abajo
# definidas, el .exe se genera igual pero sin firmar (como hasta ahora).
# Para activarla:
#   1. Da de alta una cuenta de Trusted Signing + un perfil de certificado
#      en Azure, y un service principal con el rol "Trusted Signing
#      Certificate Profile Signer" sobre ese perfil.
#   2. Define (como GitHub Secrets del repo, o variables de entorno locales
#      si lanzas este script a mano en Windows):
#        AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_CLIENT_SECRET   (el service principal)
#        TRUSTED_SIGNING_ENDPOINT       p.ej. https://eus.codesigning.azure.net/
#        TRUSTED_SIGNING_ACCOUNT        nombre de la cuenta de Trusted Signing
#        TRUSTED_SIGNING_CERT_PROFILE   nombre del perfil de certificado
# La firma incluye timestamp (RFC 3161), así que sigue siendo válida aunque
# más adelante se cancele la suscripción: solo hace falta tenerla activa en
# el momento de firmar cada build que se vaya a repartir.
$canSign = $env:AZURE_TENANT_ID -and $env:AZURE_CLIENT_ID -and $env:AZURE_CLIENT_SECRET `
  -and $env:TRUSTED_SIGNING_ENDPOINT -and $env:TRUSTED_SIGNING_ACCOUNT -and $env:TRUSTED_SIGNING_CERT_PROFILE

if ($canSign) {
    Write-Host "==> firmando $exePath (Microsoft Trusted Signing)"

    $signtool = Get-ChildItem "C:\Program Files (x86)\Windows Kits\10\bin\*\x64\signtool.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    if (-not $signtool) {
        Write-Error "No se encuentra signtool.exe (Windows SDK). Instala el 'Windows 10/11 SDK' o usa un runner que ya lo traiga (windows-latest de GitHub Actions)."
        exit 1
    }

    # Cliente de Trusted Signing (trae Azure.CodeSigning.Dlib.dll); se descarga
    # una sola vez y se cachea, como appimagetool en build-linux.sh.
    $dlibDir = Join-Path $env:LOCALAPPDATA "SunDLauncher\trusted-signing-client"
    $dlibPath = Get-ChildItem -Path $dlibDir -Recurse -Filter "Azure.CodeSigning.Dlib.dll" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $dlibPath) {
        Write-Host "    descargando Trusted Signing Client Tools (solo la primera vez)"
        New-Item -ItemType Directory -Path $dlibDir -Force | Out-Null
        $nupkg = Join-Path $env:TEMP "trusted-signing-client.zip"
        Invoke-WebRequest -Uri "https://www.nuget.org/api/v2/package/Microsoft.Trusted.Signing.Client" -OutFile $nupkg
        Expand-Archive -Path $nupkg -DestinationPath $dlibDir -Force
        $dlibPath = Get-ChildItem -Path $dlibDir -Recurse -Filter "Azure.CodeSigning.Dlib.dll" | Select-Object -First 1 -ExpandProperty FullName
    }

    # DefaultAzureCredential (usada internamente por el dlib) recoge las
    # credenciales del service principal directamente de las variables de
    # entorno AZURE_TENANT_ID/AZURE_CLIENT_ID/AZURE_CLIENT_SECRET.
    $metadataPath = Join-Path $env:TEMP "sundlauncher-trusted-signing-metadata.json"
    [ordered]@{
        Endpoint                = $env:TRUSTED_SIGNING_ENDPOINT
        CodeSigningAccountName  = $env:TRUSTED_SIGNING_ACCOUNT
        CertificateProfileName  = $env:TRUSTED_SIGNING_CERT_PROFILE
    } | ConvertTo-Json | Set-Content -Path $metadataPath -Encoding UTF8

    & $signtool sign /v /fd SHA256 /tr http://timestamp.acs.microsoft.com /td SHA256 `
        /dlib $dlibPath /dmdf $metadataPath $exePath
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Write-Host "    firmado y sellado con timestamp"
} else {
    Write-Host "==> Trusted Signing no configurado (faltan variables de entorno): $exePath se genera SIN firmar"
}

Write-Host "==> comprimiendo en zip para repartir"
$zipName = "SunDLauncher-$VERSION-windows-x64.zip"
Compress-Archive -Path $exePath -DestinationPath "target\dist\$zipName" -Force

Write-Host ""
Write-Host "Listo: target\dist\$zipName"
Write-Host "  El jugador lo descomprime y hace doble click en el .exe: instala"
Write-Host "  la app (sin JDK, con acceso directo y desinstalador)."
