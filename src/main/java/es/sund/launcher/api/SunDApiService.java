package es.sund.launcher.api;

import es.sund.launcher.exception.ApiConnectionException;
import es.sund.launcher.exception.ApiTimeoutException;
import es.sund.launcher.model.AccountCheckResponse;
import es.sund.launcher.model.GameCatalogResponse;
import es.sund.launcher.model.GameSessionTokenResponse;
import es.sund.launcher.model.VersionCheckResponse;

/**
 * Contrato de comunicación con el backend de SunD.es. Se define como interfaz
 * para poder sustituir la implementación fácilmente (por ejemplo, en tests
 * con una implementación falsa que no llame a la red real).
 */
public interface SunDApiService {

    /**
     * Verifica usuario/contraseña contra tu servidor.
     * Nota: esto NO es autenticación Mojang/Microsoft, es tu propio sistema de cuentas.
     *
     * @throws ApiTimeoutException    si el servidor no responde a tiempo
     * @throws ApiConnectionException si no se puede conectar (servidor caído, sin red, DNS, etc)
     */
    AccountCheckResponse checkAccount(String username, char[] password)
            throws ApiTimeoutException, ApiConnectionException;

    /**
     * Consulta la última versión publicada del launcher.
     *
     * @throws ApiTimeoutException    si el servidor no responde a tiempo
     * @throws ApiConnectionException si no se puede conectar (servidor caído, sin red, DNS, etc)
     */
    VersionCheckResponse checkLauncherVersion()
            throws ApiTimeoutException, ApiConnectionException;

    /**
     * Consulta el catálogo de instancias de juego disponibles (SunD Origins,
     * CobbleSpain, ...), para la pantalla de selección posterior al login.
     *
     * @throws ApiTimeoutException    si el servidor no responde a tiempo
     * @throws ApiConnectionException si no se puede conectar (servidor caído, sin red, DNS, etc)
     */
    GameCatalogResponse fetchGameCatalog()
            throws ApiTimeoutException, ApiConnectionException;

    /**
     * Pide un token de sesión de juego de un solo uso, volviendo a verificar
     * usuario/contraseña (mismo verify_login() que checkAccount, cuenta como
     * intento de login a efectos de rate-limiting). Se llama justo antes de
     * cada "Jugar", no solo al iniciar sesión en el launcher — ver
     * datos extra/Documentacion/sundauth-mod/README.md.
     *
     * @throws ApiTimeoutException    si el servidor no responde a tiempo
     * @throws ApiConnectionException si no se puede conectar (servidor caído, sin red, DNS, etc)
     */
    GameSessionTokenResponse requestGameSessionToken(String username, char[] password)
            throws ApiTimeoutException, ApiConnectionException;
}
