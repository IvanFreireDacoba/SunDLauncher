#!/usr/bin/env bash
# Genera los 3 instaladores nativos de SunDLauncher (Linux AppImage, Windows
# .exe, macOS .pkg) sin salir de este ArchLinux: sube el código a GitHub y
# dispara el workflow "Build multi-platform releases" (.github/workflows/
# release.yml), que compila con jpackage de forma NATIVA en un runner de
# cada sistema operativo (jpackage no puede cruzar plataformas, por eso
# hace falta pasar por GitHub Actions). Al terminar, descarga los 3 .zip
# resultantes en target/releases/.
#
# Flujo normal de trabajo: cambias la versión del proyecto en Eclipse/
# pom.xml, guardas, y lanzas este script. Usa esa misma versión de pom.xml
# para nombrar los 3 paquetes.
#
# Requiere: gh CLI autenticado (gh auth login) con permiso 'workflow', y
# que el repo tenga un remote 'origin' en GitHub (ver README/packaging).
#
# Uso:
#   ./packaging/build-all.sh
set -euo pipefail
cd "$(dirname "$0")/.."

if ! command -v gh >/dev/null 2>&1; then
  echo "Falta 'gh' (GitHub CLI). Instálalo (p.ej. 'sudo pacman -S github-cli')"
  echo "y ejecuta 'gh auth login' antes de volver a lanzar este script."
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "'gh' no está autenticado. Ejecuta 'gh auth login' antes de continuar."
  exit 1
fi

WORKFLOW="release.yml"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"

if [ -n "$(git status --porcelain)" ]; then
  echo "==> Hay cambios sin commitear, los subo antes de lanzar el build"
  git add src pom.xml packaging README.md .gitignore .github
  git commit -m "build: actualizar versión / empaquetado"
fi

echo "==> Pusheando a origin/$BRANCH"
git push origin "$BRANCH"

echo "==> Lanzando workflow '$WORKFLOW' en GitHub Actions"
gh workflow run "$WORKFLOW" --ref "$BRANCH"

echo "==> Esperando a que GitHub registre la nueva ejecución..."
sleep 8
RUN_ID="$(gh run list --workflow="$WORKFLOW" --branch="$BRANCH" --limit=1 --json databaseId --jq '.[0].databaseId')"
if [ -z "$RUN_ID" ]; then
  echo "No se ha encontrado la ejecución del workflow. Revísalo manualmente con:"
  echo "  gh run list --workflow=$WORKFLOW"
  exit 1
fi

echo "==> Run #$RUN_ID en marcha (compila en Linux/Windows/macOS en paralelo, ~5-10 min)"
gh run watch "$RUN_ID" --exit-status

echo "==> Descargando los 3 instaladores"
rm -rf target/releases
mkdir -p target/releases
gh run download "$RUN_ID" -D target/releases

echo
echo "Listo. Instaladores en target/releases/:"
find target/releases -maxdepth 2 -type f -name '*.zip' -exec echo "  {}" \;
