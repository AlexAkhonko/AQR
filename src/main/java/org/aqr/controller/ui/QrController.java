package org.aqr.controller.ui;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aqr.entity.Container;
import org.aqr.entity.User;
import org.aqr.service.ContainerService;
import org.aqr.service.PdfService;
import org.aqr.service.QrCodeService;
import org.aqr.service.UserService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
@RequestMapping("/qr")
@RequiredArgsConstructor
public class QrController {

    private final UserService userService;
    private final ContainerService containerService;
    private final QrCodeService qrCodeService;
    private final PdfService pdfService;

    @GetMapping("/print")
    public String qrPrintPage(Authentication auth, Model model) {User user = userService.findByLogin(auth.getName());
        model.addAttribute("containers", containerService.findByOwnerId(user.getId()));
        model.addAttribute("sizes", List.of(30, 50)); // мм
        return "qr/print";
    }

    @PostMapping("/export/zip")
    public void exportZip(@RequestParam List<Long> containerIds,
                          @RequestParam int sizeMm,
                          Authentication auth,
                          HttpServletResponse response) throws Exception {

        User user = userService.findByLogin(auth.getName());

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=qr-codes.zip");

        try (ZipOutputStream zip = new ZipOutputStream(response.getOutputStream())) {
            List<Container> containers = containerService.findOwnedByIds(user.getId(), containerIds);

            for (Container c : containers) {
                byte[] png = qrCodeService.renderQrPngBytes(c.getQrCode(), sizeMm);

                ZipEntry e = new ZipEntry("qr_" + c.getId() + ".png");
                zip.putNextEntry(e);
                zip.write(png);
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    @PostMapping(value="/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@RequestParam List<Long> containerIds,
                                            @RequestParam(defaultValue="50") int sizeMm,
                                            Authentication auth) throws Exception {
        User user = userService.findByLogin(auth.getName());
        List<Container> containers = containerService.findOwnedByIds(user.getId(), containerIds);

        byte[] pdf = pdfService.buildPdf(containers, sizeMm, c -> {
            try { return qrCodeService.renderQrPngBytes(c.getQrCode(), sizeMm); }
            catch (Exception e) { throw new RuntimeException(e); }
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr-codes.pdf")
                .cacheControl(CacheControl.noCache())
                .body(pdf);
    }

//    @GetMapping(value = "/container/{id}.png", produces = MediaType.IMAGE_PNG_VALUE)
//    public ResponseEntity<byte[]> onePng(@PathVariable Long id,
//                                         @RequestParam(defaultValue = "50") int sizeMm,
//                                         Authentication auth) throws Exception {
//        User user = userService.findByLogin(auth.getName());
//        Container c = containerService.findByIdOwned(id, user); // findByIdAndOwnerId
//
//        byte[] png = qrCodeService.renderQrPngBytes(c.getQrCode(), sizeMm);
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=qr_" + c.getId() + ".png")
//                .cacheControl(CacheControl.noCache())
//                .body(png);
//    }

    @PostMapping("/export/links")
    public String exportLinks(@RequestParam List<Long> containerIds,
                              @RequestParam(defaultValue = "50") int sizeMm,
                              Authentication auth,
                              Model model) {

        User user = userService.findByLogin(auth.getName());
        List<Container> containers = containerService.findOwnedByIds(user.getId(), containerIds);

        // DTO для ссылки
        List<QrLink> links = containers.stream()
                .map(c -> new QrLink(
                        c.getId(),
                        c.getName(),
                        "/qr/container/" + c.getId() + ".png?sizeMm=" + sizeMm
                ))
                .toList();

        model.addAttribute("links", links);
        model.addAttribute("sizeMm", sizeMm);
        return "qr/links";
    }

    @GetMapping(value = "/container/{id}.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> downloadQrPng(@PathVariable Long id,
                                                @RequestParam(defaultValue = "50") int sizeMm,
                                                Authentication auth) throws Exception {

        User user = userService.findByLogin(auth.getName());
        Container c = containerService.findOwnedByIds(user.getId(), List.of(id)).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        byte[] png = qrCodeService.renderQrPngBytes(c.getQrCode(), sizeMm);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=qr-" + id + ".png")
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }


    //todo в дто
    public record QrLink(Long id, String name, String url) {}
}
