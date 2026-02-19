package org.aqr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Breadcrumb {
    private String name;
    private String url;
    private boolean current;
}
