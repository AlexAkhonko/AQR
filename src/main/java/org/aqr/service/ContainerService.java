package org.aqr.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.aqr.dto.Breadcrumb;
import org.aqr.dto.ContainerListPage;
import org.aqr.dto.ContainerOption;
import org.aqr.entity.Container;
import org.aqr.entity.Item;
import org.aqr.entity.User;
import org.aqr.repository.ContainerRepository;
import org.aqr.utils.ImageUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ContainerService {

    private final ContainerRepository containerRepository;
    private final QrCodeService qrCodeService;
    private final StorageService storageService;
    private final BreadcrumbService breadcrumbService;
    private final ItemService itemService;
    private final UserService userService;

    public List<Container> findByOwnerId(Long ownerId) {
        return containerRepository.findByOwnerId(ownerId);
    }

    public Container findById(Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Container not found"));
    }

    public Container findByIdAndOwnerId(Long id, Long userId) {
        return containerRepository.findByIdAndOwnerId(id, userId);
    }

    public Container save(Container container) {
        return containerRepository.save(container);
    }

    public List<Container> findRootContainersByOwner(Long ownerId) {
        return containerRepository.findRootContainerByOwnerId(ownerId);
    }

    public List<Container> findChildrenByParentIdAndOwner(Long parentId, Long ownerId) {
        return containerRepository.findChildrenByParentIdAndOwner(parentId, ownerId);
    }

    public List<ContainerOption> buildOptionsForOwner(User owner) {
        List<Container> containerList = containerRepository.findByOwnerId(owner.getId());

        Map<Long, List<Container>> children = new HashMap<>();
        for (Container c : containerList) {
            Long pid = (c.getParent() == null) ? null : c.getParent().getId();
            children.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
        }
        // сортировка по имени внутри уровня (по желанию)
        children.values().forEach(list -> list.sort(Comparator.comparing(Container::getName, String.CASE_INSENSITIVE_ORDER)));

        List<ContainerOption> out = new ArrayList<>();
        dfs(null, 0, children, out);
        return out;
    }

    private void dfs(Long parentId, int level,
                     Map<Long, List<Container>> children,
                     List<ContainerOption> out) {

        for (Container c : children.getOrDefault(parentId, List.of())) {
            String indent = "—".repeat(level) + (level > 0 ? " " : "");
            out.add(new ContainerOption(c.getId(), indent + c.getName(), level));
            dfs(c.getId(), level + 1, children, out);
        }
    }

    public List<ContainerOption> buildOptionsExcludingSubtree(User owner, Long excludeRootId) {
        List<Container> all = containerRepository.findByOwnerId(owner.getId());

        // 1) Соберём set всех исключаемых id (excludeRoot + все потомки)
        Set<Long> excluded = collectSubtreeIds(all, excludeRootId);

        // 2) children map
        Map<Long, List<Container>> children = new HashMap<>();
        for (Container c : all) {
            if (excluded.contains(c.getId())) continue; // вырезаем из списка
            Long pid = (c.getParent() == null) ? null : c.getParent().getId();
            // если parent исключён, этот узел всё равно исключится через subtreeIds, но на всякий:
            if (pid != null && excluded.contains(pid)) continue;
            children.computeIfAbsent(pid, k -> new ArrayList<>()).add(c);
        }
        children.values().forEach(list -> list.sort(Comparator.comparing(Container::getName, String.CASE_INSENSITIVE_ORDER)));

        // 3) DFS → плоский список
        List<ContainerOption> out = new ArrayList<>();
        dfs(null, 0, children, out);
        return out;
    }

    private Set<Long> collectSubtreeIds(List<Container> all, Long rootId) {
        Map<Long, List<Long>> childrenIds = new HashMap<>();
        for (Container c : all) {
            Long pid = (c.getParent() == null) ? null : c.getParent().getId();
            childrenIds.computeIfAbsent(pid, k -> new ArrayList<>()).add(c.getId());
        }

        Set<Long> out = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(rootId);

        while (!stack.isEmpty()) {
            Long id = stack.pop();
            if (!out.add(id)) continue;
            for (Long ch : childrenIds.getOrDefault(id, List.of())) stack.push(ch);
        }
        return out;
    }

    @Transactional
    public void createContainerWithItems(User user,
                                         String name,
                                         String contents,
                                         MultipartFile photo,
                                         Long parentId,
                                         int cropX,
                                         int cropY,
                                         int cropS) throws Exception {

        if (photo == null || photo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "photo is required");
        }

        // 1) создаём контейнер и сохраняем, чтобы получить id
        Container container = new Container();
        container.setOwner(user);
        container.setName(name);

        // parent (owner-check)
        if (parentId != null) {
            Container parent = findByIdAndOwnerId(parentId, user.getId());
            container.setParent(parent);
        }

        // qr_code: ретраи при коллизии (предполагается unique constraint)
        for (int attempt = 0; attempt < 10; attempt++) {
            container.setQrCode(qrCodeService.newCode5());
            try {
                container = containerRepository.save(container); // id появится здесь
                break;
            } catch (DataIntegrityViolationException e) {
                if (attempt == 9) throw e;
            }
        }

        // 2) обработка изображения
        BufferedImage src = ImageIO.read(photo.getInputStream());
        if (src == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image format");
        }
        src = ImageUtil.toRgb(src);

        // 3) имя файла (id уже есть)
        String baseName = "c_" + container.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";

        // 4) original
        BufferedImage normalized = ImageUtil.resizeMax(src, 1920);
        storageService.writeJpeg(normalized, storageService.originalPath(container.getId(), baseName), 0.82f);

        // 5) square/min
        BufferedImage squareCrop = ImageUtil.cropSquareSafe(src, cropX, cropY, cropS);
        BufferedImage square512 = ImageUtil.resizeSquare(ImageUtil.toRgb(squareCrop), 512);
        storageService.writeJpeg(square512, storageService.squarePath(container.getId(), baseName), 0.80f);

        // 6) сохраняем image в БД
        container.setImage(baseName);
        containerRepository.save(container);

        // 7) создаём items из contents
        for (String line : contents.split("\\R")) {
            String itemName = line.trim();
            if (itemName.isEmpty()) continue;

            Item item = new Item();
            item.setOwner(user);
            item.setContainer(container);
            item.setText(itemName);

            itemService.save(item);
        }
    }

    @Transactional
    public ContainerListPage buildContainerContentsPage(String login, Long parentId) {
        Long userId = userService.findByLogin(login).getId();

        // owner-check: parentId должен принадлежать пользователю (иначе можно смотреть чужие контейнеры)
        //requireOwned(parentId, userId);

        List<Container> childContainers = findChildrenByParentIdAndOwner(parentId, userId);
        List<Item> items = itemService.findByContainerIdAndOwnerId(parentId, userId);
        List<Breadcrumb> breadcrumbs = breadcrumbService.buildBreadcrumbs(findById(parentId));

        return new ContainerListPage(userId, parentId, childContainers, items, breadcrumbs);
    }

    public List<Container> findOwnedByIds(Long ownerId, List<Long> containerIds) {
        return containerRepository.findByOwnerIdAndIdIn(ownerId, containerIds);
    }

//    @Transactional
//    public void updateContainer(User user, Long id, String name, Long parentId,
//                                MultipartFile photo, Integer cropX, Integer cropY, Integer cropS) throws Exception {
//        Container c = findById(id);
//        c.setName(name);
//
//        Container newParent = null;
//        if (parentId != null) {
//            newParent = findByIdOwned(parentId, user);
//        }
//
//        if (newParent != null && newParent.getId().equals(c.getId())) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Container cannot be its own parent");
//        }
//        if (newParent != null && wouldCreateCycle(c.getId(), newParent.getId(), user)) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent cannot be a descendant");
//        }
//        c.setParent(newParent);
//
//        // Фото/кроп — необязательно: если photo не прислали, ничего не меняем
//        if (photo != null && !photo.isEmpty()) {
//            if (cropX == null || cropY == null || cropS == null) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Crop is required when photo is provided");
//            }
//
//            BufferedImage src = ImageIO.read(photo.getInputStream());
//            if (src == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported image");
//
//            src = ImageUtil.toRgb(src);
//
//            // если хочешь: оставлять старое имя или генерить новое. Надёжнее — новое:
//            String baseName = "c_" + c.getId() + "_" + UUID.randomUUID().toString().substring(0, 16) + ".jpg";
//
//            BufferedImage normalized = ImageUtil.resizeMax(src, 1920);
//            storage.writeJpeg(normalized, storage.originalPath(c.getId(), baseName), 0.82f);
//
//            BufferedImage squareCrop = ImageUtil.cropSquareSafe(src, cropX, cropY, cropS);
//            BufferedImage square512 = ImageUtil.resizeSquare(ImageUtil.toRgb(squareCrop), 512);
//            storage.writeJpeg(square512, storage.squarePath(c.getId(), baseName), 0.80f);
//
//            c.setImage(baseName);
//        }
//
//        repo.save(c);
//    }
//
//    private boolean wouldCreateCycle(Long containerId, Long newParentId, User user) {
//        // идём вверх от newParent к корню, если встретим containerId — это цикл
//        Long curId = newParentId;
//        int guard = 0;
//        while (curId != null && guard++ < 1000) {
//            if (curId.equals(containerId)) return true;
//            Container cur = findByIdOwned(curId, user);
//            curId = (cur.getParent() == null) ? null : cur.getParent().getId();
//        }
//        return false;
//    }



//    public Container update(Long id, Container container, Long ownerId) {
//        Container existing = findById(id);
//        if (!existing.getOwner().getId().equals(ownerId)) {
//            //throw new AccessDeniedException("Not owner");
//        }
//        existing.setImage(container.getImage());
//        // parentContainer обновляется через setter
//        return containerRepository.save(existing);
//    }

//    public void delete(Long id, Long ownerId) {
//        Container container = findById(id);
//        if (!container.getOwner().getId().equals(ownerId)) {
//           // throw new AccessDeniedException("Not owner");
//        }
//        containerRepository.delete(container);
//    }

//    public List<Container> findChildrenByParentId(Long parentId, Long ownerId) {
//        Container parent = findById(parentId);
//        if (!parent.getOwner().getId().equals(ownerId)) {
//            throw new AccessDeniedException("Not owner");
//        }
//        return containerRepository.findChildrenByParentId(parentId);
//    }
}
