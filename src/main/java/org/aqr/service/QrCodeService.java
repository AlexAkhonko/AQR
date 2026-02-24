package org.aqr.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;

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

    public byte[] generateQrImage(String text, int size) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }

    public byte[] renderQrPngBytes(String data, int sizeMm) throws Exception {
        int px = mmToPx(sizeMm, 300);

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 0);      // 0 = без белой рамки
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = new QRCodeWriter().encode(
                data, BarcodeFormat.QR_CODE, px, px, hints
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private int mmToPx(int mm, int dpi) {
        return (int)Math.round((mm / 25.4) * dpi);
    }
}

