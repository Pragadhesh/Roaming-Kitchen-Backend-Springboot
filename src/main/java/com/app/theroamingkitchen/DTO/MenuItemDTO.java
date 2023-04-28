package com.app.theroamingkitchen.DTO;

import com.app.theroamingkitchen.models.UnitOfMeasurement;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MenuItemDTO {
    private Long id;
    private String itemName;
    private String imageUrl;
    private String amount;
    private UnitOfMeasurement unit;
}
