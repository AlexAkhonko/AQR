package org.aqr.service;

import lombok.RequiredArgsConstructor;
import org.aqr.dto.Breadcrumb;
import org.aqr.entity.Container;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BreadcrumbService {

    public List<Breadcrumb> buildBreadcrumbs(Container currentContainer) {
        List<Breadcrumb> crumbs = new ArrayList<>();
        crumbs.add(new Breadcrumb("Мои контейнеры", "/containers", false));

        if (currentContainer == null) {
            return crumbs;
        }

        Set<Long> visited = new HashSet<>();
        Deque<Container> stack = new ArrayDeque<>();

        int guard = 0;

        while (currentContainer != null && currentContainer.getId() != null && guard++ < 1000) {
            if (!visited.add(currentContainer.getId())) break;
            stack.push(currentContainer);
            currentContainer = currentContainer.getParent();
        }

        while (!stack.isEmpty()) {
            Container c = stack.pop();
            boolean isLast = stack.isEmpty();
            String url = isLast ? "" : ("/containers/" + c.getId());
            crumbs.add(new Breadcrumb(c.getName(), url, isLast));
        }

        return crumbs;
    }
}
