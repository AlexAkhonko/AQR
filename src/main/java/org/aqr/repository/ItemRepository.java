package org.aqr.repository;

import org.aqr.entity.Item;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    // ✅ Простой поиск (2 параметра)
    @Query("SELECT i FROM Item i WHERE i.owner.id = :ownerId " +
            "AND LOWER(i.text) LIKE LOWER(:text) ORDER BY i.id DESC")
    List<Item> findByOwnerIdAndTextContainingIgnoreCase(
            @Param("ownerId") Long ownerId, @Param("text") String text);

    // ✅ Последние N элементов владельца
    Page<Item> findByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);

    // ✅ Полнотекстовый поиск PostgreSQL
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
    List<Item> searchFullText(@Param("ownerId") Long ownerId,
                              @Param("searchQuery") String searchQuery);

    // Базовые запросы по владельцу
    List<Item> findByOwnerIdOrderByIdDesc(Long ownerId, PageRequest pageRequest);

    List<Item> findByOwnerIdOrderByTextAsc(Long ownerId);

    Page<Item> findByOwnerId(Long ownerId, Pageable pageable);  // ← Page + Pageable!

    // Статистика по владельцу
    @Query("SELECT COUNT(i) FROM Item i WHERE i.owner.id = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);

    // Последние N предметов
    List<Item> findTop10ByOwnerIdOrderByIdDesc(Long ownerId);

    List<Item> findByOwnerIdAndTextContainingIgnoreCaseAndIdOrderByIdDesc(Long ownerId, String text, Long id, Sort sort, Limit limit);
}