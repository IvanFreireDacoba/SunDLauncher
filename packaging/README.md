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

## Firma de código en Windows (Microsoft Trusted Signing)

Sin firmar, el `.exe` dispara el aviso de SmartScreen ("Windows protegió tu
PC" / editor no reconocido). `build-windows.ps1` ya sabe firmar con
[Microsoft Trusted Signing](https://learn.microsoft.com/azure/trusted-signing/),
pero lo hace de forma opcional: si no encuentra las variables de entorno de
abajo, genera el `.exe` sin firmar exactamente igual que hasta ahora, no
rompe el build.

Para activarla:

1. En Azure, crea una cuenta de Trusted Signing y un perfil de certificado
   (identity validation de persona física o empresa).
2. Crea un App registration (service principal) y dale el rol **"Trusted
   Signing Certificate Profile Signer"** sobre ese perfil de certificado.
3. Añade estos 6 secrets en el repo de GitHub (`Settings` → `Secrets and
   variables` → `Actions`) — el workflow (`.github/workflows/release.yml`)
   ya los pasa al job de Windows:
   - `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET` (del
     service principal del paso 2)
   - `TRUSTED_SIGNING_ENDPOINT` (p.ej. `https://eus.codesigning.azure.net/`)
   - `TRUSTED_SIGNING_ACCOUNT` (nombre de la cuenta de Trusted Signing)
   - `TRUSTED_SIGNING_CERT_PROFILE` (nombre del perfil de certificado)

La firma se sella con timestamp (RFC 3161), así que un `.exe` firmado sigue
siendo válido para siempre aunque más adelante se cancele la suscripción de
Azure — solo hace falta tenerla activa el mes en que firmes cada build que
vayas a repartir, no de forma permanente.

macOS necesitaría el equivalente con una cuenta de Apple Developer
(certificado "Developer ID Application" + notarización); no está integrado
todavía.

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
