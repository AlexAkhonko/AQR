package org.aqr.controller;

import jakarta.annotation.Resource;
import org.aqr.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file, Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        String imageUrl = imageService.uploadImage(file, ownerId);
        return ResponseEntity.ok(imageUrl);
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageId) {
        Resource resource = imageService.getImage(imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
