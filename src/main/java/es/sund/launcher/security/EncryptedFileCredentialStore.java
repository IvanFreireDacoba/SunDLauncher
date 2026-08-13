package es.sund.launcher.security;

import es.sund.launcher.config.AppPaths;
import es.sund.launcher.exception.CredentialStorageException;
import es.sund.launcher.util.OwnerOnlyFiles;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * Guarda las credenciales cifradas con AES-256-GCM en {@link AppPaths#CREDENTIALS_FILE}.
 * La clave de cifrado se genera una única vez por instalación y se guarda en
 * {@link AppPaths#CREDENTIALS_KEY_FILE}, con permisos restringidos al usuario
 * actual: permisos POSIX 600 en Linux/Mac, ACL restringida al propietario en
 * Windows (NTFS no tiene permisos POSIX, pero sí soporta ACLs vía Java NIO).
 *
 * Aviso honesto sobre el nivel de "seguridad" real: esto protege la contraseña
 * de quedar en texto plano en disco (por ejemplo, si alguien copia el fichero
 * sin la clave, o hace un backup del directorio), pero NO protege frente a
 * alguien con acceso completo a la cuenta de usuario del sistema, ya que la
 * clave vive en el mismo equipo. Es el mismo nivel de protección que usan la
 * mayoría de launchers "remember me" de escritorio.
 */
public class EncryptedFileCredentialStore implements CredentialStore {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int KEY_SIZE_BITS = 256;

    @Override
    public void save(String username, char[] password) throws CredentialStorageException {
        try {
            SecretKey key = loadOrCreateKey();
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            String payload = username + "\u0000" + new String(password);
            byte[] ciphertext = cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            byte[] fileContent = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, fileContent, 0, iv.length);
            System.arraycopy(ciphertext, 0, fileContent, iv.length, ciphertext.length);

            OwnerOnlyFiles.writeOwnerOnly(AppPaths.CREDENTIALS_FILE.toPath(), fileContent);

        } catch (GeneralSecurityException | IOException e) {
            throw new CredentialStorageException("No se pudieron guardar las credenciales de forma segura", e);
        }
    }

    @Override
    public StoredCredentials load() throws CredentialStorageException {
        if (!hasStoredCredentials()) {
            return null;
        }
        try {
            SecretKey key = loadOrCreateKey();
            byte[] fileContent = Files.readAllBytes(AppPaths.CREDENTIALS_FILE.toPath());
            if (fileContent.length < GCM_IV_LENGTH_BYTES) {
                throw new CredentialStorageException("El fichero de credenciales está corrupto");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[fileContent.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(fileContent, 0, iv, 0, iv.length);
            System.arraycopy(fileContent, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            String payload = new String(plaintext, StandardCharsets.UTF_8);
            int separatorIndex = payload.indexOf('\u0000');
            if (separatorIndex < 0) {
                throw new CredentialStorageException("El fichero de credenciales está corrupto");
            }
            String username = payload.substring(0, separatorIndex);
            char[] password = payload.substring(separatorIndex + 1).toCharArray();
            return new StoredCredentials(username, password);

        } catch (GeneralSecurityException | IOException e) {
            throw new CredentialStorageException("No se pudieron leer las credenciales guardadas", e);
        }
    }

    @Override
    public void clear() throws CredentialStorageException {
        try {
            Files.deleteIfExists(AppPaths.CREDENTIALS_FILE.toPath());
        } catch (IOException e) {
            throw new CredentialStorageException("No se pudieron eliminar las credenciales guardadas", e);
        }
    }

    @Override
    public boolean hasStoredCredentials() {
        return AppPaths.CREDENTIALS_FILE.exists();
    }

    private SecretKey loadOrCreateKey() throws GeneralSecurityException, IOException {
        Path keyPath = AppPaths.CREDENTIALS_KEY_FILE.toPath();
        if (Files.exists(keyPath)) {
            byte[] keyBytes = Files.readAllBytes(keyPath);
            return new SecretKeySpec(keyBytes, ALGORITHM);
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE_BITS);
        SecretKey key = keyGenerator.generateKey();

        OwnerOnlyFiles.writeOwnerOnly(keyPath, key.getEncoded());
        return key;
    }

}
