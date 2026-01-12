package org.aqr.controller;

import org.aqr.entity.Container;
import org.aqr.entity.User;
import org.aqr.service.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin(origins = "*")
public class ContainerController {

    @Autowired
    private ContainerService containerService;

//    @GetMapping
//    public ResponseEntity<List<Container>> getMyContainers(Authentication auth) {
//        Long ownerId = getOwnerId(auth);
//        return ResponseEntity.ok(containerService.findByOwnerId(ownerId));
//    }

    @GetMapping("/{id}")
    @PreAuthorize("#ownerId == authentication.principal.id")
    public ResponseEntity<Container> getContainer(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(containerService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Container> createContainer(
            @RequestBody Container container, Authentication auth) {
        Long ownerId = getOwnerId(auth);
        //container.setOwner(new User(ownerId));
        return ResponseEntity.ok(containerService.save(container));
    }

//    @PutMapping("/{id}")
//    public ResponseEntity<Container> updateContainer(
//            @PathVariable Long id, @RequestBody Container container, Authentication auth) {
//        Long ownerId = getOwnerId(auth);
//        return ResponseEntity.ok(containerService.update(id, container, ownerId));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteContainer(@PathVariable Long id, Authentication auth) {
//        Long ownerId = getOwnerId(auth);
//        containerService.delete(id, ownerId);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/{id}/children")
//    public ResponseEntity<List<Container>> getChildren(@PathVariable Long id, Authentication auth) {
//        Long ownerId = getOwnerId(auth);
//        return ResponseEntity.ok(containerService.findChildrenByParentId(id, ownerId));
//    }

    @PostMapping("/{id}/children")
    public ResponseEntity<Container> addChild(@PathVariable Long id,
                                              @RequestBody Container child, Authentication auth) {
        Long ownerId = getOwnerId(auth);
        //child.setParentContainer(new Container(id));
        //child.setOwner(new User(ownerId));
        return ResponseEntity.ok(containerService.save(child));
    }

    private Long getOwnerId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}

