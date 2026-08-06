package es.sund.launcher.exception;

/** Se lanza cuando falla la descarga/instalación de Minecraft, Fabric, o el lanzamiento del proceso del juego. */
public class InstallationException extends Exception {

    private static final long serialVersionUID = 5542379710604895624L;

	public InstallationException(String message) {
        super(message);
    }

    public InstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
