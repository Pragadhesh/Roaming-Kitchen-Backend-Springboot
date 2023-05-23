package com.app.theroamingkitchen.repository;

import com.app.theroamingkitchen.models.FoodDish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface FoodDishRepository extends JpaRepository<FoodDish, Long> {
    FoodDish findFirstByCatalogid(String catalogid);

    List<FoodDish> findByDishNameIn(List<String> dishNames);
}
