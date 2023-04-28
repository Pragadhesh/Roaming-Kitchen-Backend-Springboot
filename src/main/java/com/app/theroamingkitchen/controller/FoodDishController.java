package com.app.theroamingkitchen.controller;

import com.app.theroamingkitchen.DTO.FoodDishDTO;
import com.app.theroamingkitchen.service.FoodDishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class FoodDishController {

    @Autowired
    FoodDishService foodDishService;


    @GetMapping("/menu/suggestion")
    public ResponseEntity<Object> getMenuItems(@RequestBody FoodDishDTO foodDishDTO)
    {
        return foodDishService.getMenuItems(foodDishDTO);
    }


}
