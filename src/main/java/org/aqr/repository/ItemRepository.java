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

    Page<Item> findByOwnerId(Long ownerId, Pageable pageable);

    List<Item> findByOwnerId(Long ownerId);

    List<Item> findByContainerIdAndOwnerId(Long containerId, Long ownerId);

    @Query("SELECT i FROM Item i WHERE i.owner.id = :ownerId " + "AND LOWER(i.text) LIKE LOWER(:text) ORDER BY i.id DESC")
    List<Item> findByOwnerIdAndTextLikeIgnoreCase(@Param("ownerId") Long ownerId, @Param("text") String text);

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
    List<Item> searchFullText(@Param("ownerId") Long ownerId, @Param("searchQuery") String searchQuery);

    @Query("SELECT COUNT(i) FROM Item i WHERE i.owner.id = :ownerId")
    Long countByOwnerId(@Param("ownerId") Long ownerId);
}