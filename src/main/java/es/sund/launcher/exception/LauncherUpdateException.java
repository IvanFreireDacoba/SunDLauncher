package es.sund.launcher.exception;

/** Se lanza cuando falla el proceso de actualización del launcher (descarga o aplicación del pack de configuración). */
public class LauncherUpdateException extends Exception {

    private static final long serialVersionUID = -8436087779083548441L;

	public LauncherUpdateException(String message) {
        super(message);
    }

    public LauncherUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
