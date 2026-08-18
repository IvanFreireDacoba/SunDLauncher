# SunDLauncher

Launcher de escritorio para el servidor de Minecraft **SunD**. La idea es que
jugar sea tan simple como abrir una app: sin instalar Java a mano, sin buscar
la versión correcta de Fabric y sin tener que arrastrar mods uno a uno.

## Qué hace

- Instala y lanza el juego con Fabric ya configurado para cada
  instancia/modpack del servidor.
- Varias instancias se pueden instalar y jugar a la vez, cada una con su
  propia barra de progreso — no bloquea el resto del launcher.
- Login con tu cuenta de SunD, con opción de "recordarme" para no tener que
  escribir la contraseña cada vez que abres el launcher.
- Los mods se mantienen sincronizados automáticamente instancia a instancia.
- El launcher se queda minimizado mientras estás jugando y se restaura solo
  al cerrar la partida.

## Stack

Java 17 · Maven · Swing

## Compilar desde el código

Hace falta JDK 17 y Maven.

```
mvn clean package
```

Esto genera `target/SunDLauncher.jar`. Si quieres un instalador nativo con
el runtime de Java embebido, en `packaging/` hay scripts que usan
`jpackage` para Windows, macOS y Linux (uno por sistema operativo, ya que
`jpackage` no compila en cruzado).

El proyecto está importado como Maven en Eclipse; para tocar las pantallas
uso el plugin WindowBuilder.

## Descarga

En [Releases](https://github.com/IvanFreireDacoba/SunDLauncher/releases)
tienes la última versión para Windows, macOS y Linux. No hace falta tener
Java instalado, cada build lleva su propio runtime embebido.

- **Windows**: descomprime el `.zip` y ejecuta el instalador `.exe`.
- **Linux**: descomprime el `.zip` y haz doble click en el `.AppImage`.
- **macOS**: descomprime el `.zip` y ejecuta el `.pkg`. Al no estar firmado
  con un certificado de Apple, la primera vez hay que abrirlo con click
  derecho → Abrir.

## Contribuciones

Se aceptan sugerencias y peticiones de funciones — abre un
[issue](https://github.com/IvanFreireDacoba/SunDLauncher/issues).

También se aceptan **pull requests**, siempre que vengan **en una rama
nueva** (nunca directamente contra `main`).

## Licencia

Licencia propia de SunD Studios ([`LICENSE`](LICENSE)), inspirada en el
principio ShareAlike de Creative Commons: cualquier derivado debe
distribuirse con esta misma licencia y siempre de forma gratuita — nadie
puede cobrar por el software en sí (descarga, copia o instalación), aunque
sí puede usarse dentro de un servicio de pago, p. ej. un servidor con
rangos/cosméticos. Dar crédito se agradece pero no es obligatorio; lo que
sí está prohibido es apropiarse de la autoría.
