package org.aqr.controller;

import org.aqr.dto.QrCodeRequest;
import org.aqr.entity.QrCode;
import org.aqr.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qrcodes")
@CrossOrigin(origins = "*")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateQrCode(
            @RequestBody QrCodeRequest request, Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        byte[] qrImage = null;//qrCodeService.generateQrImage(request.getText(), 300, 300);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition", "attachment; filename=qr.png")
                .body(qrImage);
    }

    @GetMapping
    public ResponseEntity<List<QrCode>> getMyQrcodes(Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        return null;//ResponseEntity.ok(qrCodeService.findByOwnerId(ownerId));
    }

    @PostMapping
    public ResponseEntity<QrCode> createQrCode(
            @RequestBody QrCode qrCode, Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        //qrCode.setOwner(new User(ownerId));
        return ResponseEntity.ok(qrCodeService.save(qrCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QrCode> getQrCode(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(qrCodeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QrCode> updateQrCode(
            @PathVariable Long id, @RequestBody QrCode qrCode, Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(qrCodeService.update(id, qrCode, ownerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQrCode(@PathVariable Long id, Authentication auth) {
        Long ownerId = Long.parseLong(auth.getName());
        qrCodeService.delete(id, ownerId);
        return ResponseEntity.ok().build();
    }
}
