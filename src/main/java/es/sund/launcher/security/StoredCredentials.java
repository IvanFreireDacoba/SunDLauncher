package es.sund.launcher.security;

/** Credenciales recuperadas del almacenamiento local. Inmutable. */
public final class StoredCredentials {

    private final String username;
    private final char[] password;

    public StoredCredentials(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }
}
