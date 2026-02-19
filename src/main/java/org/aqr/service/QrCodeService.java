package org.aqr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

@Service
@Transactional
public class QrCodeService {

    private static final char[] ALPH = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom rnd = new SecureRandom();

    public String newCode5() {
        char[] out = new char[5];
        for (int i = 0; i < out.length; i++) out[i] = ALPH[rnd.nextInt(ALPH.length)];
        return new String(out);
    }

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
}

