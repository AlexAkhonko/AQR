package org.aqr.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.aqr.entity.Container;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@Service
public class PdfService {
    public byte[] buildPdf(List<Container> containers, int sizeMm, Function<Container, byte[]> pngProvider) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDFont font = PDType0Font.load(doc,
                    getClass().getResourceAsStream("/fonts/DejaVuSans.ttf"),
                    true);

            PDRectangle pageSize = PDRectangle.A4;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);

            float margin = 36f; // 0.5 inch
            float gap = 12f;
            float qrPt = mmToPt(sizeMm);
            float labelPt = 8f;
            float cellH = qrPt + 2 + labelPt + 6;

            int cols = Math.max(1, (int) Math.floor((pageSize.getWidth() - 2 * margin + gap) / (qrPt + gap)));
            float x0 = margin;
            float y = pageSize.getHeight() - margin - qrPt;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            int col = 0;
            for (Container c : containers) {
                if (col >= cols) {
                    col = 0;
                    y -= cellH;
                }
                if (y < margin + cellH) {
                    cs.close();
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageSize.getHeight() - margin - qrPt;
                }

                float x = x0 + col * (qrPt + gap);

                byte[] png = pngProvider.apply(c);
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, png, "qr");
                cs.drawImage(img, x, y, qrPt, qrPt); // drawImage для вставки изображения [web:536]

                // подпись
                String label = safeLabel(c.getName(), 24);
                cs.beginText();
                cs.setFont(font, labelPt);
                cs.newLineAtOffset(x, y - labelPt - 2);
                cs.showText(label);
                cs.endText();

                col++;
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float mmToPt(int mm) {
        return (float) (mm * 72.0 / 25.4);
    }

    private String safeLabel(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("[\\r\\n\\t]", " ").trim();
        return (s.length() <= max) ? s : s.substring(0, max - 1) + "…";
    }
}
