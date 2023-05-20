package com.app.theroamingkitchen.DTO;

import com.squareup.square.models.CatalogObject;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CatalogItemDTO {
    private String id;
    private String name;
    private String description;
    private Boolean availability;
    private String imageUrl;
    private List<CatalogObject> variations;
}
