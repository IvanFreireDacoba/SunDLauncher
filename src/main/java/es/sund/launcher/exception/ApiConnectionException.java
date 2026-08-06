package es.sund.launcher.exception;

/**
 * Se lanza cuando no se puede establecer conexión con un servidor
 * (DNS, servidor caído, sin red, certificado inválido, etc).
 * No cubre timeouts, para eso está ApiTimeoutException.
 */
public class ApiConnectionException extends Exception {

    private static final long serialVersionUID = 1L;

	public ApiConnectionException(String message) {
        super(message);
    }

    public ApiConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
