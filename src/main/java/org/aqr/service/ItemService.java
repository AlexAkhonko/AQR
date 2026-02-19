package org.aqr.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.aqr.entity.Item;
import org.aqr.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    public Page<Item> getMyItems(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return itemRepository.findByOwnerId(ownerId, pageable);
    }

//    public List<Item> getMyItems(Long ownerId) {
//        return itemRepository.findByOwnerId(ownerId);
//    }

    public List<Item> searchItems(Long ownerId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return itemRepository.findByOwnerId(ownerId);
        }

        String cleanQuery = query.trim().toLowerCase();

        // Сначала простой поиск (LIKE %query%)
        List<Item> simpleResults = itemRepository.findByOwnerIdAndTextLikeIgnoreCase(
                ownerId, "%" + cleanQuery + "%");

        if (!simpleResults.isEmpty()) {
            return simpleResults;  // ✅ Нашли!
        }

        // Полнотекстовый поиск PostgreSQL ts_rank + LIMIT
        return itemRepository.searchFullText(ownerId, query);
    }

//    @Transactional
//    public Item createItem(Long ownerId, String text) {
//
//        Item item = new Item();
//        item.setText(text.trim());
//        item.setOwner(new User(ownerId, null, null));
//
//        return itemRepository.save(item);
//    }

//    @Transactional
//    public Item updateItem(Long ownerId, Long itemId, String text) {
//        Item item = itemRepository.findById(itemId)
//                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));
//
//        // Проверка владельца
//        if (!item.getOwner().getId().equals(ownerId)) {
//            throw new AccessDeniedException("Not owner of this item");
//        }
//        item.setText(text.trim());
//
//        return itemRepository.save(item);
//    }

    @Transactional
    public void deleteItem(Long ownerId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        if (!item.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Not owner of this item");
        }

        //log.info("Deleting item {} for user {}", itemId, ownerId);
        itemRepository.delete(item);
    }

    public List<Item> findByContainerIdAndOwnerId(Long containerId, Long ownerId) {
        return itemRepository.findByContainerIdAndOwnerId(containerId, ownerId);
    }

    public void save(Item item) {
        itemRepository.save(item);
    }

//    public List<Item> findByContainerOwned(Long containerId, User user) {
//        return itemRepo.findByOwnerIdAndContainerId(user.getId(), containerId);
//    }
//
//    @Transactional
//    public void detachFromContainer(User user, Long containerId, Long itemId) {
//        Item it = itemRepo.findByIdAndOwnerId(itemId, user.getId())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//
//        if (it.getContainer() == null || !it.getContainer().getId().equals(containerId)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item not in this container");
//        }
//
//        it.setContainer(null);
//        itemRepo.save(it);
//    }
//
//    @Transactional
//    public void renameOwnedItemInContainer(User user, Long containerId, Long itemId, String text) {
//        Item it = itemRepo.findByIdAndOwnerId(itemId, user.getId())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//
//        if (it.getContainer() == null || !it.getContainer().getId().equals(containerId)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
//        }
//
//        it.setText(text.trim());
//        itemRepo.save(it);
//    }
//
//    @Transactional
//    public void moveOwnedItem(User user, Long fromContainerId, Long itemId, Long targetContainerId) {
//        Item it = itemRepo.findByIdAndOwnerId(itemId, user.getId())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
//
//        if (it.getContainer() == null || !it.getContainer().getId().equals(fromContainerId)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
//        }
//
//        Container target = containerRepo.findByIdAndOwnerId(targetContainerId, user.getId())
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target not found"));
//
//        it.setContainer(target);
//        itemRepo.save(it);
//    }

//    /**
//     * DTO для статистики
//     */
//    public record ItemStats(
//            Long totalCount,
//            int recentCount,
//            List<Item> recentItems
//    ) {}
}
