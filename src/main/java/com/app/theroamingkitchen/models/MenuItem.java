package com.app.theroamingkitchen.models;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "MenuItem")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString

public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "imageUrl",length = 1000)
    private String imageUrl;

    private Double amount;
    private UnitOfMeasurement unit;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "menu_items_food_dishes",
            joinColumns = @JoinColumn(name = "menu_item_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "food_dish_id", referencedColumnName = "id"))
    private Set<FoodDish> foodDishes = new HashSet<>();

    @OneToMany(mappedBy = "menuItem")
    private Set<MenuItemUsage> menuItemUsages = new HashSet<>();

    public MenuItem(String itemName, String imageUrl, Double amount, UnitOfMeasurement unit) {
                this.itemName = itemName;
                this.imageUrl = imageUrl;
                this.amount = amount;
                this.unit = unit;
    }
}
