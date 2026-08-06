package es.sund.launcher.security;

import es.sund.launcher.exception.CredentialStorageException;

public interface CredentialStore {

    /** Guarda usuario/contraseña de forma persistente y cifrada. */
    void save(String username, char[] password) throws CredentialStorageException;

    /** Devuelve las credenciales guardadas, o null si no hay ninguna guardada todavía. */
    StoredCredentials load() throws CredentialStorageException;

    /** Elimina cualquier credencial guardada (por ejemplo, si el login automático falla). */
    void clear() throws CredentialStorageException;

    boolean hasStoredCredentials();
}
