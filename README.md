# SunDLauncher

Launcher para Minecraft 1.20.1 + Fabric con cuentas offline, login contra tu
propia API y "recordarme" cifrado en disco.

## Importar en Eclipse

1. `File` → `Import...` → `Maven` → `Existing Maven Projects` → selecciona la
   carpeta `SunDLauncher`.
2. Instala **WindowBuilder** si no lo tienes: `Help` → `Eclipse Marketplace...`
   → busca "WindowBuilder" → `Install` → reinicia Eclipse.
3. Abre `es.sund.launcher.ui.MainFrame` → pestaña **"Design"** (abajo del
   editor) para editar visualmente el formulario.

> No he podido compilar esto en el entorno donde lo generé (sin acceso a
> Maven Central). Ábrelo en tu Eclipse con conexión normal y revisa la
> pestaña "Problems" por si hace falta algún ajuste menor.

## Por qué esta clase sí es compatible con WindowBuilder y la anterior no

WindowBuilder **instancia tu clase de verdad** para poder previsualizarla,
así que necesita:
- Un **constructor público sin argumentos** (`MainFrame`, `ProgressFrame` y
  `BackgroundPanel` ahora lo tienen). Antes `LoginFrame` exigía un
  `BiConsumer` en el constructor y WindowBuilder no podía crear la instancia.
- Que cada componente se añada con parámetros "estáticos" y trazables
  (`setBounds(...)` con literales, no un `GridBagConstraints` mutable
  reutilizado en bucle).
- Que la clase **no ejecute lógica de negocio** en el constructor (llamadas
  de red, lectura de ficheros...), porque eso se ejecutaría también dentro
  del propio editor de diseño. Por eso `MainFrame` solo construye UI; toda la
  lógica vive en `action/` y `controller/`, que se conectan desde `Main.java`
  **después** de crear la ventana.

## Arquitectura

```
config/       AppConstants (interfaz, TODOS los hardcodes) + AppPaths (rutas por SO)
exception/    Excepciones específicas: ApiConnectionException, ApiTimeoutException,
              InstallationException, LauncherUpdateException, CredentialStorageException
model/        AccountCheckResponse, VersionCheckResponse (mapeo Gson de tu API)
api/          SunDApiService (interfaz) + HttpSunDApiService (implementación HTTP)
security/     CredentialStore (interfaz) + EncryptedFileCredentialStore (AES-256-GCM)
service/      GameSessionStarter (instala+lanza el juego), LauncherUpdateService
minecraft/    MinecraftInstaller, FabricInstaller, GameLauncher (sin cambios de fondo,
              ahora lanzan InstallationException en vez de IOException genérica)
ui/           MainFrame, BackgroundPanel, ProgressFrame — SOLO construcción de UI
action/       LoginAction, ExitAction, UpdateLauncherAction — un botón, una clase
controller/   StartupController — precarga credenciales + comprueba actualización al arrancar
Main.java     único sitio donde se instancian las implementaciones concretas
```

**Regla de dependencia**: `ui/` nunca llama a `api/`, `security/` ni
`minecraft/` directamente. Solo expone getters de sus componentes. Son las
clases de `action/` y `controller/` las que reciben la `MainFrame` y la
manipulan desde fuera. Esto es justo lo que pediste como "encapsular las
acciones en sus clases necesarias".

## Interfaz de hardcodes: `AppConstants`

Todo lo que antes estaba disperso (URLs, versión del launcher, timeouts)
vive ahora solo en `config/AppConstants.java`. Incluye:

```java
String CURRENT_LAUNCHER_VERSION = "1.0";
double MAIN_WINDOW_SCREEN_RATIO = 0.60;
int CONNECT_TIMEOUT_SECONDS = 8;
int REQUEST_TIMEOUT_SECONDS = 10;
```

Ajusta aquí la versión cuando publiques una nueva, y la ventana/timeouts si
quieres otro comportamiento.

## Botón "Actualizar lanzador": cuándo se habilita

`StartupController.checkForUpdate()` llama a `CheckLauncherVersion` en un
hilo aparte nada más arrancar:

- Si hay **timeout** (`ApiTimeoutException`) o **fallo de conexión**
  (`ApiConnectionException`) → el botón se queda **deshabilitado**, sin
  interrumpir el arranque ni mostrar un diálogo molesto.
- Si responde correctamente y `latestVersion` **coincide** con
  `AppConstants.CURRENT_LAUNCHER_VERSION` → deshabilitado (ya estás al día).
- Si responde correctamente y **no coincide** → habilitado.

Al pulsarlo, `UpdateLauncherAction` vuelve a comprobar (por si la conexión
cambió entre el arranque y el click), aplica el pack de configuración si
`forceConfigUpdate=true`, y abre `launcherDownloadUrl` en el navegador si tu
API la proporciona.

## Credenciales "recordadas": cómo funciona y sus límites reales

`EncryptedFileCredentialStore` cifra usuario+contraseña con **AES-256-GCM**
antes de escribirlos en `credentials.dat`. La clave vive en `.credkey`, en la
misma carpeta de datos del launcher, con permisos `600` (solo tu usuario) en
Linux/Mac.

Sé honesto sobre esto: cifrar protege frente a que alguien copie el fichero
sin más y lo lea en texto plano (por ejemplo, si haces un backup del
directorio y se filtra), pero **no** protege frente a alguien con acceso
completo a tu cuenta de usuario del sistema, porque la clave está en el
mismo sitio. Es el mismo nivel de "recuérdame" que usan la mayoría de
launchers de escritorio (TLauncher incluido) — no es una bóveda bancaria,
es evitar el error básico de guardar la contraseña en claro.

Si en algún momento tu API empieza a devolver un **token de sesión** en vez
de pedir usuario+contraseña en cada arranque, es preferible guardar ese
token en lugar de la contraseña — dímelo y lo adapto.

## Step 1: saltar el login si el launcher está actualizado y hay sesión guardada

Al arrancar, `LauncherBootstrapper` comprueba en segundo plano, en este orden,
antes de decidir qué pantalla mostrar:

1. ¿Hay credenciales guardadas? Si no, login manual directamente.
2. ¿El launcher está actualizado? (mismo `CheckLauncherVersion` de siempre). Si
   hay una versión distinta disponible, se fuerza login manual — así el
   jugador ve el botón "Actualizar lanzador" en vez de saltárselo sin más.
3. ¿Las credenciales guardadas siguen siendo válidas contra tu API? Si el
   servidor las rechaza (contraseña cambiada, cuenta baneada...), se borran
   del almacenamiento local y se pide login manual.

Solo si las tres pasan, se salta `MainFrame` por completo y se va directo a
`GameLaunchCoordinator` (la misma clase que usa el login manual para instalar
y lanzar el juego — la extraje de `LoginAction` para no duplicarla).

Cualquier fallo de red (timeout o sin conexión) en cualquiera de las
comprobaciones cae a login manual por seguridad, en vez de asumir que todo
está bien. Es el mismo criterio "fail-safe" que ya usábamos para el botón de
actualizar.

Nota de rendimiento menor: si el bootstrapper decide mostrar login manual
tras comprobar la versión, `StartupController` volverá a comprobarla otra vez
(para habilitar el botón "Actualizar"). Es una llamada HTTP extra y barata;
si en el futuro quieres evitarla del todo, se puede pasar el resultado ya
obtenido en vez de volver a pedirlo — dímelo cuando lleguemos a pulir esto.



### `POST /APIs/CheckServerAccount`
```json
// Request
{ "username": "Fulanito", "password": "..." }
// Response
{ "success": true, "message": null, "displayName": "Fulanito" }
```

### `GET /APIs/CheckLauncherVersion`
```json
{
  "latestVersion": "1.1",
  "forceConfigUpdate": false,
  "configPackUrl": "https://sund.es/downloads/config-pack.zip",
  "launcherDownloadUrl": "https://sund.es/downloads/SunDLauncher.jar"
}
```

## La imagen de fondo

`src/main/resources/images/background.png` es un placeholder blanco liso
(1280×800). Sustitúyelo por el tuyo con el mismo nombre y ruta — `BackgroundPanel`
lo escala automáticamente al tamaño de la ventana. Si el fichero no existe o
falla al cargar, hace fallback a blanco liso sin lanzar ningún error.
