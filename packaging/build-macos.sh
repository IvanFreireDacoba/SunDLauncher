#!/usr/bin/env bash
# Empaqueta SunDLauncher como una app-image de macOS (SunDLauncher.app), con
# un runtime de Java 21 completo embebido: el usuario final no necesita
# tener Java instalado. El jugador descomprime el .zip y arrastra
# SunDLauncher.app a su carpeta ~/Aplicaciones (o /Applications): sin
# instalador, sin admin.
#
# Cambiado de --type pkg a --type app-image el 2026-08-14: un .pkg instala
# en /Applications y requiere contraseña de administrador (dominio de
# sistema), lo que habría hecho imposible que el propio launcher se
# autoactualizara sin interrumpir con ese permiso cada vez. Una app-image en
# la carpeta del propio usuario se puede sustituir en sitio sin admin, igual
# que el AppImage de Linux (ver SelfUpdateService).
#
# Requiere un JDK 21+ (con jpackage) en macOS.
#
# Nota: la app no está firmada con un certificado de Apple Developer, así
# que Gatekeeper la bloqueará como "de un desarrollador no identificado". El
# jugador tiene que hacer clic derecho -> Abrir la primera vez (o aprobarlo
# en Ajustes del Sistema > Privacidad y seguridad).
#
# La versión se toma de <version> en pom.xml.
#
# Uso:
#   ./packaging/build-macos.sh
#
# Salida: target/dist/SunDLauncher-<version>-macos.zip
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v jpackage >/dev/null 2>&1; then
  echo "No se encuentra 'jpackage' en el PATH. Necesitas un JDK 21+ y JAVA_HOME configurado."
  exit 1
fi

echo "==> leyendo versión de pom.xml"
VERSION="$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)"
echo "    versión: $VERSION"

echo "==> mvn clean package"
mvn -q clean package

echo "==> jpackage (app-image, runtime completo embebido)"
rm -rf target/dist target/jpackage-input
mkdir -p target/jpackage-input
cp target/SunDLauncher.jar target/jpackage-input/
# Resolver symlinks de $JAVA_HOME antes de pasarlo a --runtime-image: jpackage
# copia el runtime con Files.walkFileTree sin seguir symlinks, y si la raíz
# es un symlink la trata como fichero en vez de directorio (visto en CI Linux
# con el hostedtoolcache de GitHub Actions; fix defensivo también aquí). Se
# usa "cd + pwd -P" en vez de "readlink -f" porque el readlink de macOS (BSD)
# no soporta -f.
JAVA_HOME="$(cd "${JAVA_HOME:?Define JAVA_HOME apuntando a tu JDK 21}" && pwd -P)"
jpackage \
  --type app-image \
  --name SunDLauncher \
  --input target/jpackage-input \
  --main-jar SunDLauncher.jar \
  --main-class es.sund.launcher.Main \
  --icon packaging/icons/SunDLauncher.icns \
  --app-version "$VERSION" \
  --vendor "SunDStudios" \
  --mac-package-identifier es.sund.launcher \
  --runtime-image "${JAVA_HOME:?Define JAVA_HOME apuntando a tu JDK 21}" \
  --dest target/dist

echo "==> comprimiendo en zip para repartir (SunDLauncher.app entera, no solo un fichero)"
ZIP_NAME="SunDLauncher-${VERSION}-macos.zip"
(cd target/dist && zip -qr "$ZIP_NAME" "SunDLauncher.app")

echo
echo "Listo: target/dist/$ZIP_NAME"
echo "  El jugador lo descomprime y arrastra SunDLauncher.app a su carpeta"
echo "  Aplicaciones (sin JDK, sin instalador, sin admin). Al no estar"
echo "  firmada, la primera vez tendrá que hacer clic derecho -> Abrir."
