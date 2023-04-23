package com.app.theroamingkitchen.models;

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

    @ManyToMany(mappedBy = "foodDishes")
    private Set<MenuItem> menuItems = new HashSet<>();
}
