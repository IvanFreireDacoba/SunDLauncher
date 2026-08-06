package es.sund.launcher.exception;

/**
 * Se lanza específicamente cuando una llamada a la API tarda más de lo
 * permitido (ver AppConstants.CONNECT_TIMEOUT_SECONDS / REQUEST_TIMEOUT_SECONDS).
 * Es un subtipo de ApiConnectionException para poder capturar ambas juntas
 * cuando no interesa distinguirlas, o solo esta cuando sí interesa.
 */
public class ApiTimeoutException extends ApiConnectionException {

    private static final long serialVersionUID = -8138699105197554539L;

	public ApiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
