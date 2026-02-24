package org.aqr.repository;

import org.aqr.entity.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    List<Container> findByOwnerIdOrderByIdDesc(Long ownerId);

    List<Container> findByOwnerIdAndIdIn(Long ownerId, Collection<Long> ids);

    @Query("SELECT c FROM Container c WHERE c.parent.id = :parentId AND c.owner.id = :ownerId")
    List<Container> findChildrenByParentIdAndOwner(@Param("parentId") Long parentId, @Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Container c WHERE c.owner.id = :ownerId AND (:parentId IS NULL OR c.parent = :parentId)")
    List<Container> findByOwnerIdAndParentId(@Param("ownerId") Long ownerId, @Param("parentId") Long parentId);

    @Query("SELECT c FROM Container c WHERE c.owner.id = :ownerId AND c.parent.id IS NULL")
    List<Container> findRootContainerByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Container c WHERE c.owner.id = :ownerId")
    List<Container> findByOwnerId(@Param("ownerId") Long ownerId);

    //@Query("SELECT c FROM Container c WHERE c.owner.id = :ownerId")
    Container findByIdAndOwnerId(@Param("id") Long id, @Param("ownerId") Long ownerId);

//    @Query("""
//        WITH RECURSIVE container_tree AS (
//            SELECT id, parent_container_id, owner_id, 0 as level
//            FROM containers
//            WHERE id = :rootId AND owner_id = :ownerId
//
//            UNION ALL
//
//            SELECT c.id, c.parent_container_id, c.owner_id, ct.level + 1
//            FROM containers c
//            INNER JOIN container_tree ct ON c.parent_container_id = ct.id
//            WHERE c.owner_id = :ownerId
//        )
//        SELECT c FROM Container c JOIN container_tree ct ON c.id = ct.id ORDER BY ct.level
//        """)
//    List<Container> findContainerTree(@Param("rootId") Long rootId, @Param("ownerId") Long ownerId);

    @Modifying
    @Query("UPDATE Container c SET c.parent = null WHERE c.id = :id")
    void removeParent(@Param("id") Long id);
}


