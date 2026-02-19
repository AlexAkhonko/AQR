package org.aqr.repository;

import org.aqr.entity.Item;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с сущностью Item.
 * Предоставляет методы для поиска, фильтрации и статистики по элементам.
 *
 * Основные функции:
 * - Поиск элементов по владельцу с различными условиями
 * - Полнотекстовый поиск по содержимому текста
 * - Пагинация и сортировка
 * - Статистические запросы
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // =============================================
    // БАЗОВЫЕ ЗАПРОСЫ ПО ВЛАДЕЛЬЦУ
    // =============================================

    /**
     * Находит все элементы указанного владельца с пагинацией.
     *
     * @param ownerId идентификатор владельца
     * @param pageable параметры пагинации и сортировки
     * @return страница с элементами
     */
    Page<Item> findByOwnerId(Long ownerId, Pageable pageable);

    List<Item> findByOwnerId(Long ownerId);

    /**
     * Находит все элементы указанного владельца, отсортированные по ID в порядке убывания.
     *
     * @param ownerId идентификатор владельца
     * @param pageRequest параметры пагинации
     * @return список элементов
     */
    List<Item> findByOwnerIdOrderByIdDesc(Long ownerId, PageRequest pageRequest);

    // =============================================
    // ПОИСК ПО СОДЕРЖИМОМУ
    // =============================================

    /**
     * Простой поиск элементов по владельцу и тексту (регистронезависимый).
     * Использует LIKE с точным совпадением по переданному тексту.
     *
     * @param ownerId идентификатор владельца
     * @param text текст для поиска (должен содержать символы % для шаблона)
     * @return список элементов, отсортированных по ID в порядке убывания
     */
    @Query("SELECT i FROM Item i WHERE i.owner.id = :ownerId " +
            "AND LOWER(i.text) LIKE LOWER(:text) ORDER BY i.id DESC")
    List<Item> findByOwnerIdAndTextLikeIgnoreCase(
            @Param("ownerId") Long ownerId,
            @Param("text") String text);

    /**
     * Полнотекстовый поиск с использованием PostgreSQL tsvector/tsquery.
     * Поиск учитывает морфологию русского языка и ранжирует результаты по релевантности.
     *
     * @param ownerId идентификатор владельца
     * @param searchQuery поисковый запрос
     * @return список элементов, отсортированных по релевантности (от высокой к низкой)
     */
    @Query(value = """
            SELECT * FROM items 
            WHERE owner_id = :ownerId 
            AND to_tsvector('russian', text) 
            @@ plainto_tsquery('russian', :searchQuery)
            ORDER BY ts_rank(
                to_tsvector('russian', text), 
                plainto_tsquery('russian', :searchQuery)
            ) DESC
            """, nativeQuery = true)
    List<Item> searchFullText(
            @Param("ownerId") Long ownerId,
            @Param("searchQuery") String searchQuery);

    // =============================================
    // СПЕЦИАЛЬНЫЕ ЗАПРОСЫ
    // =============================================

    /**
     * Находит последние 10 элементов владельца, отсортированных по ID в порядке убывания.
     *
     * @param ownerId идентификатор владельца
     * @return список из не более чем 10 последних элементов
     */
    List<Item> findTop10ByOwnerIdOrderByIdDesc(Long ownerId);

    // =============================================
    // СТАТИСТИЧЕСКИЕ ЗАПРОСЫ
    // =============================================

    /**
     * Подсчитывает количество элементов у указанного владельца.
     *
     * @param ownerId идентификатор владельца
     * @return количество элементов
     */
    @Query("SELECT COUNT(i) FROM Item i WHERE i.owner.id = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);

    List<Item> findByContainerId(Long containerId);
}