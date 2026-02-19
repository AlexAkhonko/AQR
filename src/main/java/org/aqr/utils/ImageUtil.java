package org.aqr.utils;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class ImageUtil {
    private ImageUtil(){}

    public static BufferedImage toSquare(BufferedImage src, int size) {
        int w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (h - side) / 2;

        BufferedImage cropped = src.getSubimage(x, y, side, side);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    public static BufferedImage resizeMax(BufferedImage src, int maxSide) {
        int w = src.getWidth(), h = src.getHeight();
        int longSide = Math.max(w, h);
        if (longSide <= maxSide) return toRgb(src);

        double k = (double) maxSide / (double) longSide;
        int nw = (int)Math.round(w * k);
        int nh = (int)Math.round(h * k);

        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    public static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    public static BufferedImage cropSquareSafe(BufferedImage src, int x, int y, int s) {
        int w = src.getWidth(), h = src.getHeight();
        int maxS = Math.min(w, h);

        s = Math.max(50, Math.min(s, maxS));
        x = Math.max(0, Math.min(x, w - s));
        y = Math.max(0, Math.min(y, h - s));

        return src.getSubimage(x, y, s, s);
    }

    public static BufferedImage resizeSquare(BufferedImage square, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(square, 0, 0, size, size, null);
        g.dispose();
        return out;
    }
}

