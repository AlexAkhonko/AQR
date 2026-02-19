package org.aqr.controller;

import org.aqr.entity.Container;
import org.aqr.entity.User;
import org.aqr.service.ContainerService;
import org.aqr.service.StorageService;
import org.aqr.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

@Controller
@RequestMapping("/media")
public class MediaController {

    private final ContainerService containerService;
    private final UserService userService;
    private final StorageService storageService;

    public MediaController(ContainerService containerService, UserService userService, StorageService storageService) {
        this.containerService = containerService;
        this.userService = userService;
        this.storageService = storageService;
    }

    @GetMapping("/containers/{id}/original")
    public ResponseEntity<Resource> original(@PathVariable Long id, Authentication auth) throws IOException {
        Container c = requireOwnedContainer(id, auth);

        Path file = storageService.originalPath(c.getId(), c.getImage()).toAbsolutePath().normalize();
        return serveFileJpeg(file);
    }

    @GetMapping("/containers/{id}/square")
    public ResponseEntity<Resource> square(@PathVariable Long id, Authentication auth) throws IOException {
        Container c = requireOwnedContainer(id, auth);

        Path file = storageService.squarePath(c.getId(), c.getImage()).toAbsolutePath().normalize();
        return serveFileJpeg(file);
    }

    private Container requireOwnedContainer(Long containerId, Authentication auth) {
        User u = userService.findByLogin(auth.getName());
        Container c = containerService.findById(containerId);

        if (c == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!c.getOwner().getId().equals(u.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        return c;
    }

    private ResponseEntity<Resource> serveFileJpeg(Path file) throws IOException {
        // защита: файл обязан быть внутри uploads (реализуй baseDir в StorageService)
        if (!storageService.isInsideUploads(file)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Resource r = new UrlResource(file.toUri());
        if (!r.exists()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noCache()) // можно настроить
                .body(r);
    }
}

