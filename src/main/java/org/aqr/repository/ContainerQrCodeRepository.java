package org.aqr.repository;

import org.aqr.entity.QrCode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContainerQrCodeRepository {

    @Query("SELECT q FROM QrCode q WHERE q.container.id = :containerId AND q.owner.id = :ownerId")
    List<QrCode> findQrCodesByContainer(@Param("containerId") Long containerId, @Param("ownerId") Long ownerId);
}

