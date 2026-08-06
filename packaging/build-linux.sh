#!/usr/bin/env bash
# Empaqueta SunDLauncher como un único ejecutable AppImage para Linux, con
# un runtime de Java 17 completo embebido dentro: el usuario final NO
# necesita tener Java instalado, ni descomprimir una carpeta con un bin/
# dentro — descomprime el .zip y hace doble click en el .AppImage.
#
# Requiere solo el JDK 17 que ya usas en Eclipse (jpackage viene incluido
# desde el JDK 14, no hace falta instalar nada aparte). appimagetool se
# descarga automáticamente la primera vez (se cachea en ~/.cache).
#
# La versión del paquete se toma de <version> en pom.xml: para publicar una
# versión nueva, cambia la versión en Eclipse/pom.xml y vuelve a lanzar este
# script (o packaging/build-all.sh para las 3 plataformas a la vez).
#
# Uso:
#   ./packaging/build-linux.sh
#
# Salida: target/dist/SunDLauncher-<version>-linux-x64.zip
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v jpackage >/dev/null 2>&1; then
  echo "No se encuentra 'jpackage' en el PATH. Viene incluido en el JDK 14+"
  echo "(el mismo JDK 17 que ya usas para compilar el proyecto), asegúrate"
  echo "de que JAVA_HOME apunta a esa instalación."
  exit 1
fi

echo "==> leyendo versión de pom.xml"
VERSION="$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)"
echo "    versión: $VERSION"

echo "==> mvn clean package"
mvn -q clean package

echo "==> jpackage (app-image, runtime completo embebido)"
rm -rf target/dist target/jpackage-input target/AppDir
mkdir -p target/jpackage-input
cp target/SunDLauncher.jar target/jpackage-input/
# jpackage copia el runtime-image recorriendo el árbol con Files.walkFileTree
# sin seguir symlinks: si $JAVA_HOME es (o pasa por) un symlink -como en el
# hostedtoolcache de los runners de GitHub Actions-, trata la raíz como un
# fichero en vez de un directorio y falla con NoSuchFileException. Resolver
# a la ruta real evita el problema.
RUNTIME_IMAGE="$(readlink -f "${JAVA_HOME:?Define JAVA_HOME apuntando a tu JDK 17}")"
jpackage \
  --type app-image \
  --name SunDLauncher \
  --input target/jpackage-input \
  --main-jar SunDLauncher.jar \
  --main-class es.sund.launcher.Main \
  --icon packaging/icons/SunDLauncher.png \
  --app-version "$VERSION" \
  --vendor "SunDStudios" \
  --runtime-image "$RUNTIME_IMAGE" \
  --dest target/dist

echo "==> empaquetando como AppImage (ejecutable único)"
APPDIR="target/AppDir"
mkdir -p "$APPDIR"
cp -r target/dist/SunDLauncher/* "$APPDIR"/
cp packaging/icons/SunDLauncher.png "$APPDIR/SunDLauncher.png"

cat > "$APPDIR/SunDLauncher.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=SunDLauncher
Exec=SunDLauncher
Icon=SunDLauncher
Categories=Game;
Terminal=false
EOF

cat > "$APPDIR/AppRun" <<'EOF'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/bin/SunDLauncher" "$@"
EOF
chmod +x "$APPDIR/AppRun"

APPIMAGETOOL="$HOME/.cache/sundlauncher-build-tools/appimagetool-x86_64.AppImage"
if [ ! -x "$APPIMAGETOOL" ]; then
  echo "==> descargando appimagetool (solo la primera vez, se cachea)"
  mkdir -p "$(dirname "$APPIMAGETOOL")"
  curl -sL -o "$APPIMAGETOOL" \
    https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage
  chmod +x "$APPIMAGETOOL"
fi

# --appimage-extract-and-run evita depender de FUSE (necesario en runners de CI)
"$APPIMAGETOOL" --appimage-extract-and-run "$APPDIR" \
  "target/dist/SunDLauncher-x86_64.AppImage" >/dev/null

echo "==> comprimiendo en zip para repartir"
ZIP_NAME="SunDLauncher-${VERSION}-linux-x64.zip"
(cd target/dist && zip -q -j "$ZIP_NAME" "SunDLauncher-x86_64.AppImage")

echo
echo "Listo: target/dist/$ZIP_NAME"
echo "  El jugador lo descomprime y hace doble click en el .AppImage:"
echo "  sin JDK, sin instalación, sin carpetas."
