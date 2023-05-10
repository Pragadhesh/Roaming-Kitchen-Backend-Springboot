package com.app.theroamingkitchen.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "FoodDish")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class FoodDish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dish_name")
    private String dishName;

    @Column(name="catalogid")
    private String catalogid;

    @Column(name = "imageUrl",length = 1000)
    private String imageUrl;

    @JsonIgnore
    @ManyToMany(mappedBy = "foodDishes")
    private Set<MenuItem> menuItems = new HashSet<>();

    public FoodDish(String name, String id,String url)
    {
        this.dishName = name;
        this.catalogid = id;
        this.imageUrl = url;
    }

}
