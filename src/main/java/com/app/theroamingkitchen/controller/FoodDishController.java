package com.app.theroamingkitchen.controller;

import com.app.theroamingkitchen.DTO.CatalogDTO;
import com.app.theroamingkitchen.DTO.FoodDishDTO;
import com.app.theroamingkitchen.DTO.UpdateRecipeDTO;
import com.app.theroamingkitchen.service.CatalogService;
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

    @Autowired
    CatalogService catalogService;


    @PostMapping("/menu/suggestion")
    public ResponseEntity<Object> getMenuItems(@RequestBody FoodDishDTO foodDishDTO)
    {
        return foodDishService.getMenuItems(foodDishDTO);
    }

    @PostMapping("/menu/images")
    public ResponseEntity<Object> getMenuImages(@RequestBody FoodDishDTO foodDishDTO)
    {
        return foodDishService.getImagesforDescription(foodDishDTO);
    }

    @PostMapping("/menu/item")
    public ResponseEntity<Object> createCatalogObject(@RequestBody CatalogDTO catalogDTO)
    {
        return catalogService.createCatalogObject(catalogDTO);
    }


    @GetMapping("/menu/recipes")
    public ResponseEntity<Object> getAllRecipes()
    {
        return foodDishService.getAllFoodDish();
    }

    @GetMapping("/menu/availablerecipes")
    public ResponseEntity<Object> getAllAvailableRecipes()
    {
        return catalogService.fetchAllCatalogItems();
    }


    @GetMapping("/menu/recipes/{catalogid}")
    public ResponseEntity<Object> fetchRecipeDetails(@PathVariable("catalogid") String catalogid) {
        FoodDishDTO foodDishDTO = new FoodDishDTO();
        foodDishDTO.setCatalogId(catalogid);
        return catalogService.fetchCatalogItemDetails(foodDishDTO);
    }

    @PostMapping("/menu/recipes")
    public ResponseEntity<Object> updateRecipeDetails(@RequestBody UpdateRecipeDTO updateRecipeDTO){
        return catalogService.updateCatalogItem(updateRecipeDTO);
    }

    @GetMapping("/catalog")
    public ResponseEntity<Object> fetchAllCatalogItems()
    {
        return catalogService.fetchAllCatalogItems();
    }

}
