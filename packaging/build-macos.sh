#!/usr/bin/env bash
# Empaqueta SunDLauncher como un instalador .pkg nativo de macOS, con un
# runtime de Java 21 completo embebido: el usuario final no necesita tener
# Java instalado. Doble click en el .pkg -> instala la app en /Applications.
#
# Requiere un JDK 21+ (con jpackage) en macOS. Las herramientas de firma de
# Xcode (pkgbuild/productbuild) ya vienen con macos-latest en GitHub Actions.
#
# Nota: el .pkg no está firmado con un certificado de Apple Developer, así
# que Gatekeeper lo bloqueará como "de un desarrollador no identificado". El
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

echo "==> jpackage (instalador .pkg, runtime completo embebido)"
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
  --type pkg \
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

echo "==> comprimiendo en zip para repartir"
ZIP_NAME="SunDLauncher-${VERSION}-macos.zip"
(cd target/dist && zip -q -j "$ZIP_NAME" "SunDLauncher-${VERSION}.pkg")

echo
echo "Listo: target/dist/$ZIP_NAME"
echo "  El jugador lo descomprime y hace doble click en el .pkg: instala la"
echo "  app en /Applications (sin JDK). Al no estar firmado, la primera vez"
echo "  tendrá que hacer clic derecho -> Abrir."
