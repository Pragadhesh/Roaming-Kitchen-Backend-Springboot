package com.app.theroamingkitchen.DTO;

import com.app.theroamingkitchen.models.FoodDish;

import com.app.theroamingkitchen.models.MenuItem;
import lombok.*;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateCatalogItemDTO {
    private FoodDish foodDish;
    private Set<MenuItem> menuitems;
    private Boolean availability;
}
