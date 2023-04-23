package com.app.theroamingkitchen.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class FoodDishDTO {
    private long id;
    private String dishName;
    private String catalogId;
}
