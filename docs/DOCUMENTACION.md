# Documentación técnica — SunDLauncher

Referencia rápida de la estructura del código. Para el detalle completo (parámetros, salidas, excepciones y cuerpo de cada método) ver la documentación HTML generada para staff — este fichero es un mapa de navegación del repositorio, no un sustituto.

**65 clases/interfaces · 307 métodos/constructores** en `src/main/java/es/sund/launcher/`.

## Arquitectura, en una frase por paso

`Main` (composition root) → `LauncherBootstrapper` decide si hace falta login manual (`MainFrame`/`LoginAction`) o se salta a la selección de instancias → `InstanceSelectionFrame` pinta el catálogo (`GET /APIs/GameCatalog`) → cada tarjeta (`InstancePanel`) tiene un único botón (`PlayOrInstallAction`) que instala/actualiza/juega según `InstanceInstallStatus` → `GameLaunchCoordinator` decide instalar vía `GameSessionStarter` (Minecraft) o `NativeGameInstaller` (otros juegos) o lanzar el proceso real. Cada instancia corre en su propio hilo, en paralelo con las demás.

## Dependencias

- `com.google.code.gson:gson` 2.11.0 — serialización JSON de la API.
- `maven-shade-plugin` 3.5.1 — empaquetado del JAR ejecutable único.
- Resto: JDK 17 estándar (`java.net.http`, Swing/AWT, `javax.crypto`, `java.util.prefs`) — sin más dependencias externas.

## `es.sund.launcher`

Raíz — composition root de la aplicación

### `class Main` — Punto de entrada y composition root: es el único sitio de toda la aplicación

```java
void main(String[] args);
```

## `es.sund.launcher.action`

Manejadores de eventos (`ActionListener`) de los botones de la interfaz

### `class ExitAction` — Cierra la aplicación. Es su propia clase por consistencia y para poder añadir lógica de cierre limpio más adelante.

```java
void actionPerformed(ActionEvent e);
```

### `class LoginAction` — Encapsula todo lo que ocurre al pulsar "Entrar": 1) verifica la cuenta contra

```java
LoginAction(MainFrame mainFrame, SunDApiService apiService, CredentialStore credentialStore, Consumer<String> onLoginSuccess);
void actionPerformed(ActionEvent e);
```

### `class LogoutAction` — Cierra la sesión actual: borra las credenciales guardadas (para que

```java
LogoutAction(InstanceSelectionFrame frame, CredentialStore credentialStore, Runnable onLoggedOut);
void actionPerformed(ActionEvent e);
```

### `class OpenLocalFilesAction` — Abre AppPaths.ROOT_DIR (carpeta de datos del launcher: instancias, credenciales, config) en el explorador de archivos del sistema.

```java
OpenLocalFilesAction(Component parent);
void actionPerformed(ActionEvent e);
```

### `class PlayOrInstallAction` — Botón único por instancia que sirve tanto para "Instalar"/"Actualizar" como

```java
PlayOrInstallAction(InstanceSelectionFrame frame, InstancePanel panel, GameInstance instance, String username, SunDApiService apiService, CredentialStore credentialStore);
void actionPerformed(ActionEvent e);
```

### `class UninstallAction` — Borra del disco todo lo instalado de una instancia (para Minecraft: versión

```java
UninstallAction(InstancePanel panel, GameInstance instance);
void actionPerformed(ActionEvent e);
```

### `class UpdateLauncherAction` — Encapsula la actualización del launcher. Solo debería poder pulsarse cuando

```java
UpdateLauncherAction(MainFrame mainFrame, LauncherUpdateService updateService);
UpdateLauncherAction(MainFrame mainFrame, LauncherUpdateService updateService, SelfUpdateService selfUpdateService);
void actionPerformed(ActionEvent e);
```

## `es.sund.launcher.api`

Cliente HTTP contra el backend de sund.es

### `class HttpSunDApiService`

```java
AccountCheckResponse checkAccount(String username, char[] password) throws ApiTimeoutException, ApiConnectionException;
VersionCheckResponse checkLauncherVersion() throws ApiTimeoutException, ApiConnectionException;
GameCatalogResponse fetchGameCatalog() throws ApiTimeoutException, ApiConnectionException;
GameSessionTokenResponse requestGameSessionToken(String username, char[] password) throws ApiTimeoutException, ApiConnectionException;
```

### `interface SunDApiService` — Contrato de comunicación con el backend de SunD.es. Se define como interfaz

```java
AccountCheckResponse checkAccount(String username, char[] password) throws ApiTimeoutException, ApiConnectionException;
VersionCheckResponse checkLauncherVersion() throws ApiTimeoutException, ApiConnectionException;
GameCatalogResponse fetchGameCatalog() throws ApiTimeoutException, ApiConnectionException;
GameSessionTokenResponse requestGameSessionToken(String username, char[] password) throws ApiTimeoutException, ApiConnectionException;
```

## `es.sund.launcher.config`

Constantes, rutas de instalación y ajustes persistentes

### `interface AppConstants` — Todos los valores "hardcodeados" de la aplicación viven aquí, y solo aquí.

_(sin métodos públicos — clase de datos o de uso interno)_

### `class AppPaths` — Rutas de ficheros/carpetas del launcher. A diferencia de AppConstants, esto

```java
InstancePaths forInstance(GameInstance instance);
```

### `class LauncherSettings` — Ajustes del launcher que persisten entre arranques (a diferencia de

```java
boolean isMinimizeDuringGameEnabled();
void setMinimizeDuringGameEnabled(boolean enabled);
String getLauncherTheme();
void setLauncherTheme(String themeId);
```

## `es.sund.launcher.controller`

Orquestación del arranque y del catálogo de instancias

### `class InstanceCatalogController` — Pide en segundo plano el catálogo de instancias (GET /APIs/GameCatalog) para

```java
InstanceCatalogController(SunDApiService apiService);
void load(Consumer<List<GameInstance>> onLoaded, Consumer<String> onFailure);
```

### `class LauncherBootstrapper` — Se ejecuta una única vez al arrancar la aplicación, ANTES de decidir qué

```java
LauncherBootstrapper(CredentialStore credentialStore, SunDApiService apiService, LauncherUpdateService updateService);
void run(Runnable onManualLoginRequired, Consumer<String> onAutoLoginSuccess);
```

### `class StartupController` — Se ejecuta una vez al arrancar la aplicación, antes de mostrar la ventana

```java
StartupController(MainFrame mainFrame, CredentialStore credentialStore, SunDApiService apiService, LauncherUpdateService updateService);
void onStartup();
```

## `es.sund.launcher.exception`

Excepciones propias tipadas

### `class ApiConnectionException` — Se lanza cuando no se puede establecer conexión con un servidor

```java
ApiConnectionException(String message);
ApiConnectionException(String message, Throwable cause);
```

### `class ApiTimeoutException` — Se lanza específicamente cuando una llamada a la API tarda más de lo

```java
ApiTimeoutException(String message, Throwable cause);
```

### `class CredentialStorageException` — Se lanza cuando falla el guardado, lectura o cifrado/descifrado de las credenciales almacenadas localmente.

```java
CredentialStorageException(String message);
CredentialStorageException(String message, Throwable cause);
```

### `class InstallationException` — Se lanza cuando falla la descarga/instalación de Minecraft, Fabric, o el lanzamiento del proceso del juego.

```java
InstallationException(String message);
InstallationException(String message, Throwable cause);
```

### `class LauncherUpdateException` — Se lanza cuando falla el proceso de actualización del launcher (descarga o aplicación del pack de configuración).

```java
LauncherUpdateException(String message);
LauncherUpdateException(String message, Throwable cause);
```

## `es.sund.launcher.minecraft`

Instalación y lanzamiento de instancias Minecraft/Fabric

### `class FabricInstaller` — Instala Fabric Loader sobre una versión vanilla ya instalada, usando meta.fabricmc.net.

```java
FabricInstaller(AppPaths.InstancePaths paths, ProgressListener listener);
String fabricVersionId(String mcVersion, String loaderVersion);
boolean isInstalled(AppPaths.InstancePaths paths, String mcVersion, String loaderVersion);
JsonObject install(String mcVersion, String loaderVersion) throws InstallationException;
```

### `class GameLauncher` — Une la versión vanilla + el profile de Fabric (si lo hay) y lanza el proceso Java.

```java
Process launch(AppPaths.InstancePaths paths, String versionId, JsonObject vanillaJson, JsonObject fabricJson, String username, String uuid) throws InstallationException;
```

### `class InstanceContentInstaller` — Instala el contenido propio de una instancia que no viene de Mojang/Fabric:

```java
InstanceContentInstaller(AppPaths.InstancePaths paths, ProgressListener listener);
String readAppliedInstancePackSha1(AppPaths.InstancePaths paths);
String readAppliedModpackJsonSha1(AppPaths.InstancePaths paths);
String readAppliedResourcepackJsonSha1(AppPaths.InstancePaths paths);
void install(GameInstance instance) throws InstallationException;
FileVisitResult visitFile(Path source, BasicFileAttributes attrs) throws IOException;
```

### `class MinecraftInstaller` — Descarga e instala una versión vanilla de Minecraft usando las APIs públicas

```java
MinecraftInstaller(AppPaths.InstancePaths paths, ProgressListener listener);
boolean isInstalled(AppPaths.InstancePaths paths, String versionId);
JsonObject install(String versionId) throws InstallationException;
```

### `class ModrinthClient` — Resuelve mods/resourcepacks a su fichero descargable real contra la API

```java
ModrinthVersionResponse.ModrinthFile resolvePrimaryFile(String versionId) throws IOException, InterruptedException, InstallationException;
ModrinthVersionResponse.ModrinthFile resolveResourcepackFile(String modrinthProjectUrl, String targetVersion, String mcVersion) throws IOException, InterruptedException, InstallationException;
```

### `class NbtServersFile` — Lector/escritor NBT mínimo, suficiente para leer y escribir un servers.dat

_(sin métodos públicos — clase de datos o de uso interno)_

### `class ServerListMerger` — Fusiona el servers.dat que trae el instance-pack (con la dirección de SunD

```java
void mergeInto(Path incomingServersDat, Path targetServersDat) throws IOException;
```

## `es.sund.launcher.model`

DTOs de las respuestas de la API (deserializados con Gson)

### `class AccountCheckResponse` — Respuesta esperada de POST /APIs/CheckServerAccount.

_(sin métodos públicos — clase de datos o de uso interno)_

### `class GameCatalogResponse` — Respuesta esperada de GET /APIs/GameCatalog.

_(sin métodos públicos — clase de datos o de uso interno)_

### `class GameInstance` — Una instancia de juego del catálogo (SunD Origins, CobbleSpain, ...), tal y

```java
boolean isNative();
```

### `class GameSessionTokenResponse` — Respuesta de POST /APIs/GameSessionToken. Token de un solo uso, de vida

_(sin métodos públicos — clase de datos o de uso interno)_

### `class ModpackDefinition` — Respuesta esperada de GET {GameInstance.modpackJsonUrl} (un JSON estático,

_(sin métodos públicos — clase de datos o de uso interno)_

### `class ModrinthVersionResponse` — Subconjunto de la respuesta de GET https://api.modrinth.com/v2/version/{id}

_(sin métodos públicos — clase de datos o de uso interno)_

### `class ResourcepackDefinition` — Respuesta esperada de GET {GameInstance.resourcepackJsonUrl} (JSON estático).

_(sin métodos públicos — clase de datos o de uso interno)_

### `class VersionCheckResponse` — Respuesta esperada de GET /APIs/CheckLauncherVersion.

_(sin métodos públicos — clase de datos o de uso interno)_

## `es.sund.launcher.nativegame`

Instancias de juego no-Minecraft (p. ej. PocketCrossing)

### `class NativeGameInstaller` — Instala una instancia que NO es Minecraft (ver GameInstance.isNative()): descarga

```java
NativeGameInstaller(AppPaths.InstancePaths paths, ProgressListener listener);
boolean isInstalled(AppPaths.InstancePaths paths);
void install(GameInstance instance) throws InstallationException;
```

### `class NativeGameLauncher` — Lanza el proceso de un cliente nativo (no Minecraft) ya instalado por NativeGameInstaller:

```java
Process launch(AppPaths.InstancePaths paths, GameInstance instance, String username) throws InstallationException;
```

## `es.sund.launcher.security`

Almacenamiento de credenciales y tokens de sesión

### `interface CredentialStore`

```java
void save(String username, char[] password) throws CredentialStorageException;
StoredCredentials load() throws CredentialStorageException;
void clear() throws CredentialStorageException;
boolean hasStoredCredentials();
```

### `class EncryptedFileCredentialStore` — Guarda las credenciales cifradas con AES-256-GCM en {@link AppPaths#CREDENTIALS_FILE}.

```java
void save(String username, char[] password) throws CredentialStorageException;
StoredCredentials load() throws CredentialStorageException;
void clear() throws CredentialStorageException;
boolean hasStoredCredentials();
```

### `class GameSessionTokenFile` — Escribe el token de sesión de juego de un solo uso que recoge el mod

```java
void write(AppPaths.InstancePaths instancePaths, String token, String minecraftUsername) throws IOException;
void deleteIfExists(AppPaths.InstancePaths instancePaths);
```

### `class StoredCredentials` — Credenciales recuperadas del almacenamiento local. Inmutable.

```java
StoredCredentials(String username, char[] password);
String getUsername();
char[] getPassword();
```

## `es.sund.launcher.service`

Lógica de negocio: lanzar el juego, autoactualización, etc.

### `class GameLaunchCoordinator` — Encapsula el flujo de "instalar lo que falte y lanzar el juego" para una

```java
void launch(String username, GameInstance instance, InstancePanel panel, InstanceSelectionFrame frame, SunDApiService apiService, CredentialStore credentialStore, Consumer<String> onFailure);
```

### `class GameSessionStarter` — Encapsula el flujo completo de "asegurar que el juego está instalado y lanzarlo"

```java
GameSessionStarter(GameInstance instance, ProgressListener progressListener, SunDApiService apiService, CredentialStore credentialStore);
void ensureInstalled() throws InstallationException;
Process start(String username) throws InstallationException;
```

### `class InstanceInstallStatus` — Comprueba en disco si una instancia (Minecraft o nativa, ver GameInstance.isNative()) ya está instalada, sin tocar la red.

```java
boolean isInstalled(GameInstance instance);
boolean isReadyToPlay(GameInstance instance);
boolean isUpdateAvailable(GameInstance instance);
void refreshPanel(InstancePanel panel, GameInstance instance);
```

### `class LaunchActivityTracker` — Cuenta cuántas instancias tienen ahora mismo una partida de Minecraft

```java
boolean beginAndWasFirst();
boolean endAndWasLast();
```

### `class LauncherUpdateService`

```java
LauncherUpdateService(SunDApiService apiService);
VersionCheckResponse checkRemoteVersion() throws ApiTimeoutException, ApiConnectionException;
boolean isUpdateAvailable(VersionCheckResponse remote);
void applyUpdate(VersionCheckResponse remote) throws LauncherUpdateException;
```

### `class SelfUpdateService` — Descarga la release "Latest" del propio launcher directamente desde GitHub y la aplica

```java
Result performSelfUpdate(DownloadUtil.ProgressListener progress) throws LauncherUpdateException;
```

## `es.sund.launcher.ui`

Componentes Swing de la interfaz gráfica

### `class BackgroundPanel` — JPanel que pinta una imagen de fondo escalada para llenar todo el panel.

```java
BackgroundPanel();
```

### `class ImageScaling` — Dibuja imágenes preservando su proporción real, en vez del

_(sin métodos públicos — clase de datos o de uso interno)_

### `class InstanceGridTile` — Franja de una instancia dentro de la columna izquierda (sustituye a la

```java
InstanceGridTile();
void setInstanceName(String name);
void setBackgroundImageResource(String resourcePath);
void setLogoImageResource(String resourcePath);
void setSelected(boolean selected);
Dimension getPreferredSize();
Dimension getMinimumSize();
Dimension getMaximumSize();
```

### `class InstanceIcons` — Carga la miniatura (logo) de una instancia para la columna vertical de

_(sin métodos públicos — clase de datos o de uso interno)_

### `class InstanceListItem` — Fila de la columna vertical de instancias (logo + nombre), dentro de

```java
InstanceListItem();
void setLogoIcon(Icon icon);
void setInstanceName(String name);
void setSelected(boolean selected);
```

### `class InstancePanel` — Panel de detalle de una instancia dentro de InstanceSelectionFrame: la

```java
InstancePanel();
JButton getActionButton();
JButton getUninstallButton();
void setInstanceInfo(String name, String details);
void showInstalled();
void showUpdateAvailable();
void showNotInstalled();
void showPlaying();
void showProgress(String taskName, long bytesDone, long bytesTotal);
void setBackgroundImageResource(String resourcePath);
void setLogoImageResource(String resourcePath);
void setPokedexStyle(boolean pokedexStyle);
```

### `class InstanceSelectionFrame` — Pantalla de selección de instancias (estilo Riot Client), mostrada tras el

```java
InstanceSelectionFrame();
void mouseClicked(java.awt.event.MouseEvent e);
void setUsername(String username);
void showInstances(List<GameInstance> instances);
InstancePanel getInstancePanel(int instanceId);
ProfileScreen getProfileScreen();
void setStatus(String text);
```

### `class MainFrame` — Ventana principal. Esta clase SOLO construye y expone componentes de UI:

```java
MainFrame();
void componentResized(ComponentEvent e);
void insertUpdate(DocumentEvent e);
void removeUpdate(DocumentEvent e);
void changedUpdate(DocumentEvent e);
void mouseClicked(MouseEvent e);
JTextField getUsernameField();
JPasswordField getPasswordField();
JButton getLoginButton();
JButton getExitButton();
JButton getUpdateButton();
void setStatus(String text);
void setStatusWithAccountHint(String text);
void setFormEnabled(boolean enabled);
void setUpdateButtonEnabled(boolean enabled);
void showSelfUpdateProgress(String taskName, long bytesDone, long bytesTotal);
void hideSelfUpdateProgress();
void prefillCredentials(String username, char[] password);
void prefillCredentialsAndAutoLogin(String username, char[] password);
```

### `class ProfileScreen` — Pantalla de "tu perfil" en el área de detalle de InstanceSelectionFrame:

```java
ProfileScreen();
void setUsername(String username);
JButton getLogoutButton();
JButton getOpenLocalFilesButton();
```

### `class ProgressFrame`

```java
ProgressFrame();
void update(String taskName, long done, long total);
void setTask(String text);
```

### `class RoundedPanel` — Panel con fondo de piedra semitransparente y borde dorado, esquinas

_(sin métodos públicos — clase de datos o de uso interno)_

### `class ScrollableStackPanel` — JPanel normal y corriente salvo por una cosa: implementa Scrollable para

```java
Dimension getPreferredScrollableViewportSize();
int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction);
int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction);
boolean getScrollableTracksViewportWidth();
boolean getScrollableTracksViewportHeight();
```

### `class Theme` — Paleta de color del launcher. Tres temas seleccionables desde ProfileScreen

_(sin métodos públicos — clase de datos o de uso interno)_

## `es.sund.launcher.util`

Utilidades genéricas (descargas, imágenes, UUIDs, permisos)

### `class DownloadUtil`

```java
String getString(String url) throws IOException, InterruptedException;
void downloadFile(String url, Path destination, String expectedSha1, String taskName, ProgressListener listener) throws IOException, InterruptedException;
void downloadFile(String url, Path destination, String hashAlgorithm, String expectedHashHex, String taskName, ProgressListener listener) throws IOException, InterruptedException;
void unzip(File zipFile, File targetDir) throws IOException;
Path resolveChild(Path targetRoot, String name) throws IOException;
void deleteRecursive(Path path) throws IOException;
```

### `class IcoImageLoader` — Lector mínimo del formato .ico de Windows, sin dependencias externas (el

```java
List<BufferedImage> loadAllSizes(String classpathResource);
```

### `class OfflineUUID` — Genera un UUID offline exactamente igual que lo hace el cliente vanilla de Minecraft

```java
UUID generate(String username);
```

### `class OwnerOnlyFiles` — Escribe ficheros restringidos al usuario propietario (permisos POSIX 600 en

```java
void writeOwnerOnly(Path path, byte[] content) throws IOException;
void restrictToOwnerOnly(Path path);
```

---
_Generado a partir del código fuente real del proyecto._
