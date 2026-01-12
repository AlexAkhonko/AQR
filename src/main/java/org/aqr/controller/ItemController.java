package org.aqr.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.aqr.entity.Item;
import org.aqr.entity.User;
import org.aqr.service.ItemService;
import org.aqr.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Управление описаниями содержимого")
public class ItemController {

    private final ItemService itemService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<Item>> getMyItems(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.findByLogin(auth.getName());
        return ResponseEntity.ok(itemService.getMyItems(user.getId(), page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Item>> searchItems(
            Authentication auth,
            @RequestParam String q) {
        User user = userService.findByLogin(auth.getName());
        return ResponseEntity.ok(itemService.searchItems(user.getId(), q));
    }

    @PostMapping
    public ResponseEntity<Item> createItem(
            Authentication auth,
            @RequestBody @Valid CreateItemRequest request) {
        User user = userService.findByLogin(auth.getName());
        return ResponseEntity.ok(itemService.createItem(user.getId(), request.text()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody @Valid CreateItemRequest request) {
        User user = userService.findByLogin(auth.getName());
        return ResponseEntity.ok(itemService.updateItem(user.getId(), id, request.text()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            Authentication auth,
            @PathVariable Long id) {
        User user = userService.findByLogin(auth.getName());
        itemService.deleteItem(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<ItemService.ItemStats> getStats(Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(itemService.getStats(ownerId));
    }
}

record CreateItemRequest(@NotBlank String text) {}