package org.aqr.service;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageService {
    private static final String UPLOAD_DIR = "uploads/";

    public String uploadImage(MultipartFile file, Long ownerId) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return "/api/images/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }

    public Resource getImage(String imageId) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(imageId);
//            Resource resource = new UrlResource(filePath.toUri());
//            if (resource.exists()) return resource;
            throw new EntityNotFoundException("Image not found");
        } catch (Exception e) {
            throw new RuntimeException("Image load failed", e);
        }
    }
}