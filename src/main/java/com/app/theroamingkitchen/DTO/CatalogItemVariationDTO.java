package com.app.theroamingkitchen.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CatalogItemVariationDTO {
    private String id;
    private String name;
    private int priceAmount;
}
