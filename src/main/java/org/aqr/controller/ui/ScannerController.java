package org.aqr.controller.ui;

import lombok.RequiredArgsConstructor;
import org.aqr.entity.Container;
import org.aqr.entity.User;
import org.aqr.service.ContainerService;
import org.aqr.service.ItemService;
import org.aqr.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ScannerController {

    private final ItemService itemService;
    private final ContainerService containerService;
    private final UserService userService;

    @GetMapping("/scan")
    public String scanner(Model model, Authentication auth) {
        User user = userService.findByLogin(auth.getName());
        Long ownerId = user.getId();  // твой метод
        // Все айтемы юзера с картинками
        List<Container> userContainers = containerService.findByOwnerId(ownerId);

        Map<String, String> qrToImage = userContainers.stream()
                .filter(container -> container.getQrCode() != null)
                .collect(Collectors.toMap(
                        Container::getQrCode,  // "car456"
                        container -> String.format("/media/containers/%d/square",
                                container.getId())  // /items/1/car.jpg
                ));

        model.addAttribute("qrToImage", qrToImage);
        return "scanner";
    }

    private Long getOwnerId(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}
