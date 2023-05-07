package com.app.theroamingkitchen.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CatalogDTO {
    private String dishName;
    private String description;
    private String imageUrl;
    private List<ItemVariationDTO> variations;
    private List<MenuItemResultDTO> ingredients;
}
