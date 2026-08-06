# Empaquetado con runtime de Java embebido (jpackage)

Hoy `mvn package` solo produce un `.jar` shaded (`target/SunDLauncher.jar`):
requiere que el usuario final tenga Java 17+ instalado para poder
ejecutarlo (`java -jar SunDLauncher.jar`). Los scripts de esta carpeta usan
`jpackage` (incluido en el JDK desde la versión 14, no es una herramienta
aparte) para producir en su lugar un ejecutable nativo por SO con un
runtime de Java 17 completo **embebido dentro** — el usuario no necesita
tener Java instalado en absoluto.

## Requisitos

Solo el JDK 17 que ya usas para compilar el proyecto en Eclipse, con
`JAVA_HOME` apuntando a él. `jpackage` viene incluido ahí, no hay que
instalar nada más para producir el app-image (la carpeta/`.app`/`.exe`
autocontenida). Generar además un instalador de verdad con asistente
(`.msi`/`.exe` en Windows, `.dmg`/`.pkg` en Mac, `.deb`/`.rpm` en Linux) sí
necesita herramientas extra del SO — ver el comentario dentro de cada
script para cuáles.

## Uso

`jpackage` no puede cruzar plataformas: genera un paquete nativo solo para
el sistema operativo en el que se ejecuta. Por eso, para conseguir los 3
instaladores (Linux/Windows/macOS) desde una sola máquina Linux, se usa
GitHub Actions (`.github/workflows/release.yml`), que compila de forma
nativa en un runner de cada SO.

**Flujo normal**: cambia la versión en `pom.xml` (Eclipse), guarda, y
lanza:

```
./packaging/build-all.sh
```

Esto sube los cambios a GitHub, dispara el workflow, espera a que termine
en los 3 sistemas en paralelo y descarga los 3 `.zip` en
`target/releases/`:

- `SunDLauncher-<version>-linux-x64.zip` — un `.AppImage` (ejecutable
  único, doble click y listo, sin instalación).
- `SunDLauncher-<version>-windows-x64.zip` — un instalador `.exe` (doble
  click, instala con acceso directo y desinstalador).
- `SunDLauncher-<version>-macos.zip` — un instalador `.pkg` (doble click,
  instala en `/Applications`; al no estar firmado, la primera vez hay que
  hacer clic derecho → Abrir).

Los 3 llevan el runtime de Java 17 completo embebido: el jugador no
necesita tener Java instalado.

Si alguna vez tienes acceso directo a una máquina Windows o Mac, también
puedes lanzar `packaging/build-windows.ps1` o `packaging/build-macos.sh`
ahí mismo, sin pasar por GitHub Actions.

## Iconos

`icons/SunDLauncher.ico` (Windows), `.icns` (macOS) y `.png` (Linux) son
placeholders generados, listos para sustituirse por arte real con el mismo
nombre de fichero cuando lo haya.
`src/main/resources/images/SunDLauncher.ico` (el que usa la propia
aplicación en tiempo de ejecución, vía `IcoImageLoader`, para el icono de
ventana/barra de tareas) es el mismo fichero — si lo sustituyes, actualiza
las dos copias.

## Por qué `--runtime-image "$JAVA_HOME"` y no un runtime recortado con jlink

Los scripts embeben el JDK **completo**, no una imagen recortada por
`jlink` con solo los módulos que se detecten como necesarios. Es más
pesado (embebe todo el JDK en vez de solo unos 40-60 MB), pero es la
opción segura: `jlink`/jpackage detectan los módulos necesarios analizando
el `.jar` con `jdeps`, y ese análisis puede no detectar bien módulos que
solo se cargan por reflexión (Gson serializa/deserializa así; Swing/AWT
también tira de módulos opcionales de forma dinámica según el SO). Con el
JDK completo no hay riesgo de un `ClassNotFoundException`/`NoClassDefFoundError`
en tiempo de ejecución por un módulo que faltaba. Si el tamaño del
instalador importa, recortar el runtime con `jlink` es un paso opcional
para más adelante, pero hay que probarlo a fondo en cada SO antes de
repartirlo así.
