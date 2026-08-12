package es.sund.launcher.ui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Dibuja imágenes preservando su proporción real, en vez del
 * {@code g.drawImage(img, 0, 0, w, h, this)} de toda la vida que las estira
 * (deforma) al ancho/alto exactos del componente. Dos modos, como el CSS
 * {@code background-size}:
 * <ul>
 * <li>{@link #drawCover}: llena el rectángulo entero recortando el sobrante
 * (fondos de instancia, pantalla completa).</li>
 * <li>{@link #drawContain}: cabe entera dentro del rectángulo sin recortar
 * (logos, que no se pueden perder ni un pixel).</li>
 * </ul>
 */
final class ImageScaling {

    private ImageScaling() {}

    /** Recorte centrado ("cover"): llena {@code w}x{@code h} sin deformar, recortando el sobrante. */
    static void drawCover(Graphics2D g2, BufferedImage image, int w, int h) {
        if (image == null || w <= 0 || h <= 0) {
            return;
        }
        int imgW = image.getWidth();
        int imgH = image.getHeight();
        double targetAspect = (double) w / h;
        double sourceAspect = (double) imgW / imgH;
        int sx, sy, sw, sh;
        if (sourceAspect > targetAspect) {
            sh = imgH;
            sw = (int) Math.round(imgH * targetAspect);
            sx = (imgW - sw) / 2;
            sy = 0;
        } else {
            sw = imgW;
            sh = (int) Math.round(imgW / targetAspect);
            sx = 0;
            sy = (imgH - sh) / 2;
        }
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, w, h, sx, sy, sx + sw, sy + sh, null);
    }

    /**
     * Ajuste centrado ("contain"): cabe entera dentro de {@code w}x{@code h} sin
     * deformar ni recortar, dejando margen si hace falta. {@code maxFraction}
     * limita además el tamaño máximo relativo al rectángulo disponible (p.ej.
     * 0.6 = como mucho el 60% del ancho/alto), para que un logo no tape el
     * fondo entero en paneles grandes.
     */
    static void drawContain(Graphics2D g2, BufferedImage image, int w, int h, double maxFraction) {
        if (image == null || w <= 0 || h <= 0) {
            return;
        }
        int imgW = image.getWidth();
        int imgH = image.getHeight();
        double maxW = w * maxFraction;
        double maxH = h * maxFraction;
        double scale = Math.min(maxW / imgW, maxH / imgH);
        scale = Math.min(scale, 1.0); // nunca agrandar un logo pequeño más allá de su tamaño real
        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);
        int dx = (w - drawW) / 2;
        int dy = (h - drawH) / 2;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, dx, dy, dx + drawW, dy + drawH, 0, 0, imgW, imgH, null);
    }
}
