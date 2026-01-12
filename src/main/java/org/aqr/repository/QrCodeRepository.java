package org.aqr.repository;

import org.aqr.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Long> {

    List<QrCode> findByOwnerIdOrderByIdDesc(Long ownerId);

    List<QrCode> findByOwnerIdAndContainerId(Long ownerId, Long containerId);

    @Query("SELECT q FROM QrCode q WHERE q.code = :code AND q.owner.id = :ownerId")
    Optional<QrCode> findByCodeAndOwner(@Param("code") String code, @Param("ownerId") Long ownerId);

    @Query(value = "SELECT * FROM qr_codes WHERE code = :code", nativeQuery = true)
    Optional<QrCode> findByCodePublic(@Param("code") String code);
}

