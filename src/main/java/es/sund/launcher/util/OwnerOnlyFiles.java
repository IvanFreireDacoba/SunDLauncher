package es.sund.launcher.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Escribe ficheros restringidos al usuario propietario (permisos POSIX 600 en
 * Linux/Mac, ACL propietario-only en Windows), extraído de
 * EncryptedFileCredentialStore para reutilizarlo también con el fichero de
 * token de sesión de juego (ver GameSessionTokenFile) sin duplicar esta
 * lógica multiplataforma.
 */
public final class OwnerOnlyFiles {

    private OwnerOnlyFiles() {}

    /**
     * Escribe contenido en un fichero nuevo con los permisos restrictivos ya aplicados desde su
     * creación (en vez de crear con los permisos por defecto del SO y restringir después). Sin
     * esto, entre el "Files.write" que crea el fichero y el "chmod"/ACL posterior había una
     * ventana breve en la que, en Linux/Mac con un umask permisivo (p.ej. 022, bastante común),
     * el fichero era mundialmente legible: otro usuario del mismo sistema con un proceso
     * vigilando el directorio (inotify) podría leerlo durante esa ventana. Al crear el fichero
     * ya con permisos de solo propietario (POSIX) no existe ese hueco. En Windows no hay
     * equivalente NIO para "crear ya con esta ACL" en una sola llamada, así que se mantiene el
     * camino de crear + restringir ACL inmediatamente después.
     */
    public static void writeOwnerOnly(Path path, byte[] content) throws IOException {
        if (!Files.exists(path)) {
            Set<String> supportedViews = path.getFileSystem().supportedFileAttributeViews();
            if (supportedViews.contains("posix")) {
                FileAttribute<Set<PosixFilePermission>> ownerOnlyAttr = PosixFilePermissions.asFileAttribute(
                        EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                Files.createFile(path, ownerOnlyAttr);
            } else {
                Files.createFile(path);
            }
        }
        Files.write(path, content);
        restrictToOwnerOnly(path);
    }

    /** Restringe el fichero al usuario propietario: permisos 600 en Linux/Mac, ACL propietario-only en Windows. */
    public static void restrictToOwnerOnly(Path path) {
        Set<String> supportedViews = path.getFileSystem().supportedFileAttributeViews();
        try {
            if (supportedViews.contains("posix")) {
                Set<PosixFilePermission> ownerOnly = EnumSet.of(
                        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(path, ownerOnly);
            } else if (supportedViews.contains("acl")) {
                restrictWithAcl(path);
            }
            // Ni POSIX ni ACL disponibles: no es crítico, queda al criterio del llamante si el
            // contenido debe ir cifrado o es de vida tan corta que no importa (ver GameSessionTokenFile).
        } catch (IOException ignored) {
            // No bloqueante.
        }
    }

    /** Windows (NTFS): sustituye la ACL del fichero por una única entrada que solo permite acceso al propietario. */
    private static void restrictWithAcl(Path path) throws IOException {
        AclFileAttributeView aclView = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (aclView == null) {
            return;
        }
        UserPrincipal owner = aclView.getOwner();
        AclEntry ownerOnlyEntry = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(AclEntryPermission.values())
                .build();
        aclView.setAcl(List.of(ownerOnlyEntry));
    }
}
