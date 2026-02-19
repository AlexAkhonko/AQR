package org.aqr.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.aqr.entity.Container;
import org.aqr.entity.Item;
import org.aqr.entity.User;
import org.aqr.repository.ItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository itemRepository;

    /**
     * Получить все предметы пользователя (последние 50, пагинация)
     */
    public Page<Item> getMyItems(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return itemRepository.findByOwnerId(ownerId, pageable);
    }

    public List<Item> getMyItems(Long ownerId) {
        return itemRepository.findByOwnerId(ownerId);
    }

    /**
     * Поиск по тексту (простой + полнотекстовый)
     */
    public List<Item> searchItems(Long ownerId, String query) {
        if (query == null || query.trim().isEmpty()) {
            // Лимит 50 последних
            return itemRepository.findByOwnerIdOrderByIdDesc(ownerId, PageRequest.of(0, 50));
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

    /**
     * Создать новый предмет
     */
    @Transactional
    public Item createItem(Long ownerId, String text) {

        Item item = new Item();
        item.setText(text.trim());
        item.setOwner(new User(ownerId, null, null));

        return itemRepository.save(item);
    }

    /**
     * Обновить предмет
     */
    @Transactional
    public Item updateItem(Long ownerId, Long itemId, String text) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Item not found: " + itemId));

        // Проверка владельца
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Not owner of this item");
        }

        //log.info("Updating item {} for user {}: {}", itemId, ownerId, text);
        item.setText(text.trim());

        return itemRepository.save(item);
    }

    /**
     * Удалить предмет
     */
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

    /**
     * Получить статистику
     */
    public ItemStats getStats(Long ownerId) {
        Long totalCount = itemRepository.countByOwnerId(ownerId);
        List<Item> recentItems = itemRepository.findTop10ByOwnerIdOrderByIdDesc(ownerId);

        return new ItemStats(totalCount, recentItems.size(), recentItems);
    }

    /**
     * Массовое создание (из фото OCR)
     */
    @Transactional
    public List<Item> createItemsFromOcr(Long ownerId, List<String> texts) {
        //log.info("Creating {} items from OCR for user {}", texts.size(), ownerId);

        return texts.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .map(text -> createItem(ownerId, text))
                .toList();
    }

    public List<Item> findByContainerId(Long containerId) {
        return itemRepository.findByContainerId(containerId);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
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

    /**
     * DTO для статистики
     */
    public record ItemStats(
            Long totalCount,
            int recentCount,
            List<Item> recentItems
    ) {}
}
