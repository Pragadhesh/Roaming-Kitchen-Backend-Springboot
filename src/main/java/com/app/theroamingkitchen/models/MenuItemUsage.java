package com.app.theroamingkitchen.models;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "MenuItemUsage")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MenuItemUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @ManyToOne
    @JoinColumn(name = "food_dish_id")
    private FoodDish foodDish;

    @Column(name = "quantity_used")
    private Double quantityUsed;

    public MenuItemUsage(MenuItem mts, FoodDish foodDish, Double amount) {
        this.menuItem = mts;
        this.foodDish = foodDish;
        this.quantityUsed = amount;
    }

}

