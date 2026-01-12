package org.aqr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.aqr.entity.Container;
import org.aqr.entity.QrCode;
import org.aqr.repository.ContainerRepository;
import org.aqr.repository.QrCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@Transactional
public class QrCodeService {

    @Autowired
    private QrCodeRepository qrCodeRepository;
    @Autowired private ContainerRepository containerRepository;

    public byte[] generateQrImage(String text, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }

//    public List<QrCode> findByOwnerId(Long ownerId) {
//        return qrCodeRepository.findByOwnerId(ownerId);
//    }

    public QrCode save(QrCode qrCode) {
        return qrCodeRepository.save(qrCode);
    }

    public QrCode findById(Long id) {
        return qrCodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("QR code not found"));
    }

    public QrCode update(Long id, QrCode qrCode, Long ownerId) {
        QrCode existing = findById(id);
//        if (!existing.getOwner().getId().equals(ownerId)) {
//            //throw new AccessDeniedException("Not owner");
//        }
//        existing.setCode(qrCode.getCode());
//        existing.setImage(qrCode.getImage());
//        if (qrCode.getContainer() != null) {
//            Container container = containerRepository.findById(qrCode.getContainer().getId())
//                    .orElseThrow(() -> new EntityNotFoundException("Container not found"));
//            existing.setContainer(container);
//        }
        return qrCodeRepository.save(existing);
    }

    public void delete(Long id, Long ownerId) {
        QrCode qrCode = findById(id);
//        if (!qrCode.getOwner().getId().equals(ownerId)) {
//            //throw new AccessDeniedException("Not owner");
//        }
        qrCodeRepository.delete(qrCode);
    }
}

