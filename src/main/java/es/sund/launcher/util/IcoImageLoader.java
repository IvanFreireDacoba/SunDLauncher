package es.sund.launcher.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector mínimo del formato .ico de Windows, sin dependencias externas (el
 * JDK no trae un ImageReader para este formato). Solo soporta el caso que
 * generan todas las herramientas modernas (icoutils, GIMP, ImageMagick+
 * png2ico, etc. desde hace más de una década): cada frame del icono
 * contiene directamente un PNG completo en vez de un DIB de Windows crudo.
 * Es el mismo formato que produce, por ejemplo, `magick`/`convert` al
 * empaquetar PNGs en un .ico, y el que exporta cualquier editor de iconos
 * actual, así que cubre el caso real sin arrastrar una librería solo para
 * esto (mismo criterio que NbtServersFile con el NBT de Minecraft).
 *
 * Si algún frame no es un PNG (DIB legado, muy raro hoy en día) se ignora en
 * vez de fallar: mejor mostrar los tamaños que sí se puedan leer que no
 * mostrar ningún icono.
 */
public final class IcoImageLoader {

    private IcoImageLoader() {}

    /**
     * Todas las resoluciones embebidas en el .ico, de mayor a menor. Java
     * (JFrame.setIconImages) elige sola la que mejor encaje según dónde se
     * use (barra de título, barra de tareas, Alt+Tab...), así que conviene
     * pasarlas todas en vez de una sola.
     */
    public static List<BufferedImage> loadAllSizes(String classpathResource) {
        List<BufferedImage> images = new ArrayList<>();
        try (InputStream in = IcoImageLoader.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                return images;
            }
            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            if (buffer.remaining() < 6) {
                return images;
            }
            buffer.getShort(); // reserved, siempre 0
            short type = buffer.getShort(); // 1 = icono, 2 = cursor
            int count = Short.toUnsignedInt(buffer.getShort());
            if (type != 1 || count <= 0 || count > 64) {
                return images; // no es un .ico de icono válido/razonable
            }

            for (int i = 0; i < count; i++) {
                if (buffer.remaining() < 16) {
                    break;
                }
                buffer.get(); // width (0 = 256, no se necesita: se lee del propio PNG)
                buffer.get(); // height
                buffer.get(); // color count
                buffer.get(); // reserved
                buffer.getShort(); // color planes
                buffer.getShort(); // bits per pixel
                int dataSize = buffer.getInt();
                int dataOffset = buffer.getInt();

                if (dataSize <= 0 || dataOffset < 0 || dataOffset + dataSize > bytes.length) {
                    continue;
                }
                if (!looksLikePng(bytes, dataOffset)) {
                    continue; // DIB crudo (BMP) sin descomprimir a mano: se ignora, no se inventa
                }
                try (InputStream frame = new ByteArrayInputStream(bytes, dataOffset, dataSize)) {
                    BufferedImage image = ImageIO.read(frame);
                    if (image != null) {
                        images.add(image);
                    }
                } catch (IOException ignored) {
                    // Un frame corrupto no debe tumbar el resto.
                }
            }
        } catch (IOException e) {
            return images;
        }

        images.sort((a, b) -> Integer.compare(b.getWidth(), a.getWidth()));
        return images;
    }

    private static boolean looksLikePng(byte[] bytes, int offset) {
        byte[] pngMagic = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
        if (offset + pngMagic.length > bytes.length) {
            return false;
        }
        for (int i = 0; i < pngMagic.length; i++) {
            if (bytes[offset + i] != pngMagic[i]) {
                return false;
            }
        }
        return true;
    }
}
