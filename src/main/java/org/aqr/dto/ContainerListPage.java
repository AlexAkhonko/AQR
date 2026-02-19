package org.aqr.dto;

import org.aqr.entity.Container;
import org.aqr.entity.Item;

import java.util.List;

public record ContainerListPage(
        Long userId,
        Long parentId,
        List<Container> containers,
        List<Item> items,
        List<Breadcrumb> breadcrumbs
) {}
