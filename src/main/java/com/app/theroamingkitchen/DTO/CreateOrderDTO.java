package com.app.theroamingkitchen.DTO;


import com.squareup.square.models.CatalogObject;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CreateOrderDTO {
    private String firstname;
    private String lastname;
    private String email;
    private String address;
    private String phone;
    private String customerlatitude;
    private String customerlongitude;
    private String storelatitude;
    private String storelongitude;
    private List<CartItemDTO> cartitems;

}
