package es.sund.launcher.exception;

/** Se lanza cuando falla el guardado, lectura o cifrado/descifrado de las credenciales almacenadas localmente. */
public class CredentialStorageException extends Exception {

    private static final long serialVersionUID = -8536722769644491527L;

	public CredentialStorageException(String message) {
        super(message);
    }

    public CredentialStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
