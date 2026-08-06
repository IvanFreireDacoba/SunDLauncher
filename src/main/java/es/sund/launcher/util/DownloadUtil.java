package es.sund.launcher.util;

import es.sund.launcher.config.AppConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
//import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class DownloadUtil {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(AppConstants.CONNECT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public interface ProgressListener {
        void onProgress(String taskName, long bytesDone, long bytesTotal);
    }

    public static String getString(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(toAbsoluteUri(url)).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " al pedir " + url);
        }
        return resp.body();
    }

    /**
     * Convierte una URL en URI validando que tenga esquema (http/https). El backend
     * siempre debería mandar URLs absolutas, pero si por una mala configuración
     * (p.ej. "home_url" sin esquema en el .env del servidor) llega una URL relativa,
     * preferimos fallar aquí con un mensaje claro y capturable (IOException) en vez
     * de dejar que URI.create lance un IllegalArgumentException sin capturar que
     * mata en silencio el hilo de instalación/lanzamiento.
     */
    private static URI toAbsoluteUri(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("URL de descarga vacía o nula.");
        }
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null) {
                throw new IOException("URL de descarga sin esquema (http/https): \"" + url
                        + "\". Revisa la configuración del servidor (home_url).");
            }
            if (uri.getHost() == null) {
                // Esquema presente pero sin "//host" (p.ej. "https:/assets/..." en vez de
                // "https://sund.es/assets/..."): HttpRequest lo rechaza con "unsupported URI"
                // en vez de conectar a ningún sitio. Mismo origen que el caso anterior: home_url
                // mal construido en el backend.
                throw new IOException("URL de descarga sin host: \"" + url
                        + "\". Revisa la configuración del servidor (home_url).");
            }
            return uri;
        } catch (IllegalArgumentException malformed) {
            throw new IOException("URL de descarga inválida: \"" + url + "\" (" + malformed.getMessage() + ")", malformed);
        }
    }

    public static void downloadFile(String url, Path destination, String expectedSha1,
                                     String taskName, ProgressListener listener) throws IOException, InterruptedException {
        if (Files.exists(destination) && expectedSha1 != null && sha1Matches(destination, expectedSha1)) {
            return;
        }

        Files.createDirectories(destination.getParent());
        Path tmp = destination.resolveSibling(destination.getFileName() + ".tmp");

        HttpRequest req = HttpRequest.newBuilder(toAbsoluteUri(url)).GET().build();
        HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("HTTP " + resp.statusCode() + " al descargar " + url);
        }

        long total = resp.headers().firstValueAsLong("Content-Length").orElse(-1);
        long done = 0;

        try (InputStream in = resp.body();
             OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                done += read;
                if (listener != null) listener.onProgress(taskName, done, total);
            }
        }

        if (expectedSha1 != null && !sha1Matches(tmp, expectedSha1)) {
            Files.deleteIfExists(tmp);
            throw new IOException("SHA1 no coincide para " + destination.getFileName());
        }

        Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static boolean sha1Matches(Path file, String expectedSha1) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(expectedSha1);
        } catch (Exception e) {
            return false;
        }
    }

    /** Extrae un zip íntegro sobre targetDir, respetando la estructura de carpetas de las entradas. */
    public static void unzip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    continue;
                }
                outFile.getParentFile().mkdirs();
                try (OutputStream os = Files.newOutputStream(outFile.toPath())) {
                    zis.transferTo(os);
                }
            }
        }
    }

    public static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });
        }
    }

    private DownloadUtil() {}
}
