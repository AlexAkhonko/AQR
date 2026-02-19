package org.aqr.controller.ui;

import lombok.RequiredArgsConstructor;
import org.aqr.dto.Breadcrumb;
import org.aqr.entity.Container;
import org.aqr.entity.Item;
import org.aqr.entity.User;
import org.aqr.service.*;
import org.aqr.utils.ImageUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/containers")
public class ContainersController {

    private final ContainerService containerService;
    private final ItemService itemService;
    private final UserService userService;
    private final QrCodeService qrCodeService;
    private final StorageService storageService;

    @GetMapping
    public String rootContainers(Authentication auth, Model model) {
        String login = auth.getName();
        Long userId = userService.findByLogin(login).getId();

        List<Container> rootContainers = containerService.findRootContainersByOwner(userId);
        List<Breadcrumb> breadcrumbs = List.of(new Breadcrumb("Мои контейнеры", "", true));

        model.addAttribute("containers", rootContainers);
        model.addAttribute("items", List.of()); // нет айтемов на корне
        model.addAttribute("breadcrumbs", breadcrumbs);
        model.addAttribute("parentId", null);
        model.addAttribute("userId", userId);

        return "containers/container_list";
    }

    @GetMapping("/{parentId}")
    public String containerContents(@PathVariable Long parentId,
                                    Authentication auth,
                                    Model model) {
        String login = auth.getName();
        Long userId = userService.findByLogin(login).getId();

        Container parent = containerService.findById(parentId);
        List<Container> childContainers = containerService.findChildren(parentId, userId);
        List<Item> items = itemService.findByContainerId(parentId);

        List<Breadcrumb> breadcrumbs = buildBreadcrumbs(parentId);

        model.addAttribute("containers", childContainers);
        model.addAttribute("items", items);
        model.addAttribute("breadcrumbs", breadcrumbs);
        model.addAttribute("parentId", parentId);
        model.addAttribute("userId", userId);

        return "containers/container_list";
    }

    private List<Breadcrumb> buildBreadcrumbs(Long currentId) {
        // TODO: рекурсивно строить путь от корня до currentId
        return List.of(
                new Breadcrumb("Мои контейнеры", "/containers", false),
                new Breadcrumb("Контейнер #" + currentId, "", true)
        );
    }

    @GetMapping("/new")
    public String newForm(Model model, Authentication auth) {
        User user = userService.findByLogin(auth.getName());
        model.addAttribute("parentOptions", containerService.buildOptionsForOwner(user));
        return "containers/container_new";
    }

    @Transactional
    @PostMapping
    public String create(@RequestParam String name,
                         @RequestParam String contents,
                         @RequestParam("photo") MultipartFile photo,
                         Authentication auth,
                         @RequestParam (required = false) Long parentId,
                         @RequestParam int cropX,
                         @RequestParam int cropY,
                         @RequestParam int cropS) throws Exception {

        User user = userService.findByLogin(auth.getName());

        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photo is required");
        }

        // 1) создаём контейнер (чтобы получить id)
        Container container = new Container();
        container.setOwner(user);
        container.setName(name);

        // qr: ретраи на случай коллизии по (owner_id, qr_code)
        for (int attempt = 0; attempt < 10; attempt++) {
            container.setQrCode(qrCodeService.newCode5());
            try {
                container = containerService.save(container); // важно: чтобы id появился
                break;
            } catch (DataIntegrityViolationException e) {
                if (attempt == 9) throw e;
            }
        }

        // 2) читаем изображение
        BufferedImage src = ImageIO.read(photo.getInputStream());
        if (src == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format");
        }
        src = ImageUtil.toRgb(src);

        // 3) формируем имена файлов (теперь id точно есть)
        String baseName = "c_" + container.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";

        // 4) сохраняем "original" в нормальном размере/весе
        BufferedImage normalized = ImageUtil.resizeMax(src, 1920);
        storageService.writeJpeg(normalized, storageService.originalPath(container.getId(), baseName), 0.82f);

        // 5) square/min по кропу пользователя (кроп в координатах ИСХОДНОГО src)
        BufferedImage squareCrop = ImageUtil.cropSquareSafe(src, cropX, cropY, cropS);
        BufferedImage square512 = ImageUtil.resizeSquare(ImageUtil.toRgb(squareCrop), 512);
        storageService.writeJpeg(square512, storageService.squarePath(container.getId(), baseName), 0.80f);

        // 6) обновляем container.image
        container.setImage(baseName);

        if (parentId != null) {
            Container parent = containerService.findByIdAndOwnerId(parentId, user.getId()); // важно: owner-check
            container.setParent(parent);
        }

        containerService.save(container);



        // 4) items из contents (по строкам)
        for (String line : contents.split("\\R")) {
            String itemName = line.trim();
            if (itemName.isEmpty()) continue;
            Item item = new Item();
            item.setOwner(user);
            item.setContainer(container);
            item.setText(itemName);
            itemService.save(item);
        }

        return "redirect:/dashboard";
    }

//    @GetMapping("/{id}/edit")
//    public String edit(@PathVariable Long id, Model model, Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//
//        Container c = containerService.findByIdOwned(id, user);
//        List<Item> items = itemService.findByContainerOwned(id, user);
//
//        model.addAttribute("container", c);
//        model.addAttribute("items", items);
//
//        // список для выбора родителя: исключаем текущий контейнер и его поддерево
//        model.addAttribute("parentOptions", containerService.buildOptionsExcludingSubtree(user, id));
//
//        // список контейнеров для "move item": можно полный, без исключений (item можно перемещать куда угодно в дереве владельца)
//        model.addAttribute("moveTargets", containerService.buildOptionsForOwner(user));
//
//        return "containers/edit";
//    }
//
//    @PostMapping("/{id}")
//    public String update(@PathVariable Long id,
//                         @RequestParam String name,
//                         @RequestParam(required=false) Long parentId,
//                         @RequestParam(required=false, name="photo") MultipartFile photo,
//                         @RequestParam(required=false) Integer cropX,
//                         @RequestParam(required=false) Integer cropY,
//                         @RequestParam(required=false) Integer cropS,
//                         Authentication auth) throws Exception {
//
//        User user = userService.findByLogin(auth.getName());
//        containerService.updateContainer(user, id, name, parentId, photo, cropX, cropY, cropS);
//
//        return "redirect:/containers/" + id + "/edit";
//    }
//
//    @PostMapping("/{id}/items/{itemId}/detach")
//    public String detachItem(@PathVariable Long id, @PathVariable Long itemId, Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//        itemService.detachFromContainer(user, id, itemId);
//        return "redirect:/containers/" + id + "/edit";
//    }
//
//    @PostMapping("/{id}/items/{itemId}/rename")
//    public String renameItem(@PathVariable Long id, @PathVariable Long itemId,
//                             @RequestParam String text,
//                             Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//        itemService.renameOwnedItemInContainer(user, id, itemId, text);
//        return "redirect:/containers/" + id + "/edit";
//    }
//
//    @PostMapping("/{id}/items/{itemId}/move")
//    public String moveItem(@PathVariable Long id, @PathVariable Long itemId,
//                           @RequestParam Long targetContainerId,
//                           Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//        itemService.moveOwnedItem(user, id, itemId, targetContainerId);
//        return "redirect:/containers/" + id + "/edit";
//    }
//
//    @GetMapping("/{id}/edit")
//    public String edit(@PathVariable Long id, Model model, Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//
//        Container c = containerService.findByIdOwned(id, user);
//        model.addAttribute("container", c);
//        model.addAttribute("items", itemService.findByContainerOwned(id, user));
//        model.addAttribute("parentOptions", containerService.buildOptionsExcludingSubtree(user, id));
//        model.addAttribute("moveTargets", containerService.buildOptionsForOwner(user));
//
//        return "containers/edit";
//    }
//
//    @PostMapping("/{id}")
//    public String update(@PathVariable Long id,
//                         @RequestParam String name,
//                         @RequestParam(required = false) Long parentId,
//                         @RequestParam(required = false) MultipartFile photo,
//                         @RequestParam(required = false) Integer cropX,
//                         @RequestParam(required = false) Integer cropY,
//                         @RequestParam(required = false) Integer cropS,
//                         Authentication auth) throws Exception {
//
//        User user = userService.findByLogin(auth.getName());
//        containerService.updateContainer(user, id, name, parentId, photo, cropX, cropY, cropS);
//        return "redirect:/containers/" + id + "/edit";
//    }
//
//    @PostMapping("/{cid}/items/{iid}/detach")
//    public String detach(@PathVariable Long cid, @PathVariable Long iid, Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//        itemService.detachFromContainer(user, cid, iid);
//        return "redirect:/containers/" + cid + "/edit";
//    }
//
//    @PostMapping("/{cid}/items/{iid}/rename")
//    public String rename(@PathVariable Long cid, @PathVariable Long iid,
//                         @RequestParam String text, Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//        itemService.renameOwnedItemInContainer(user, cid, iid, text);
//        return "redirect:/containers/" + cid + "/edit";
//    }
//
//    @PostMapping("/{cid}/items/{iid}/move")
//    public String move(@PathVariable Long cid, @PathVariable Long iid,
//                       @RequestParam Long targetContainerId, Authentication auth) {
//        User user = userService.findByLogin(auth.getName());
//        itemService.moveOwnedItem(user, cid, iid, targetContainerId);
//        return "redirect:/containers/" + cid + "/edit";
//    }

//    @Transactional
//    public void changeParent(User user, Long containerId, Long newParentId) {
//        Container c = findByIdOwned(containerId, user);
//
//        Container newParent = null;
//        if (newParentId != null) {
//            newParent = findByIdOwned(newParentId, user);
//        }
//
//        // 1) нельзя сделать родителем самого себя
//        if (newParent != null && c.getId().equals(newParent.getId())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container cannot be its own parent");
//        }
//
//        // 2) нельзя сделать родителем своего потомка (иначе цикл)
//        if (newParent != null && isDescendant(newParent, c)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent cannot be a descendant");
//        }
//
//        c.setParent(newParent);
//        containerRepository.save(c);
//    }
//
//    private boolean isDescendant(Container candidateParent, Container container) {
//        Container cur = candidateParent;
//        int guard = 0;
//
//        while (cur != null && guard++ < 1000) { // guard на случай уже битых данных
//            if (cur.getParent() == null) return false;
//            Long pid = cur.getParent().getId();
//            if (pid == null) return false;
//
//            if (pid.equals(container.getId())) return true;
//
//            // подгружаем следующего родителя (можно getReferenceById)
//            cur = containerRepository.findById(pid).orElse(null);
//        }
//        return false;
//    }
}
