package com.app.theroamingkitchen.repository;

import com.app.theroamingkitchen.models.FoodDish;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FoodDishRepository extends JpaRepository<FoodDish, Long> {
    FoodDish findFirstByCatalogid(String catalogid);
}
