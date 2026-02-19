package org.aqr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

@Service
public class StorageService {
    private final Path root;

    public StorageService(@Value("${app.uploads-dir:./uploads}") String uploadsDir) {
        this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
    }

    public Path containerDir(long containerId) throws IOException {
        Path dir = root.resolve("containers").resolve(String.valueOf(containerId)).normalize();
        Files.createDirectories(dir);
        return dir;
    }

    public Path originalPath(long containerId, String baseName) throws IOException {
        return containerDir(containerId).resolve(baseName).normalize();
    }

    public Path squarePath(long containerId, String baseName) throws IOException {
        String minName = baseName.replace(".jpg", "-min.jpg");
        return containerDir(containerId).resolve(minName).normalize();
    }

    public void writeJpeg(BufferedImage img, Path target, float quality) throws IOException {
        Files.createDirectories(target.getParent());

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IllegalStateException("No JPEG writer");

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        try (OutputStream os = Files.newOutputStream(target);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public boolean isInsideUploads(Path file) {
        Path base = root.toAbsolutePath().normalize();
        Path f = file.toAbsolutePath().normalize();
        return f.startsWith(base);
    }
}

