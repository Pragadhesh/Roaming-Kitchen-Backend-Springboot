package com.app.theroamingkitchen.DTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UpdateRecipeDTO {
    private String catalogid;
    private Boolean availability;
}
