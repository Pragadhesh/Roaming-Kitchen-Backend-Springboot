package com.app.theroamingkitchen.controller;

import com.app.theroamingkitchen.DTO.MenuItemDTO;
import com.app.theroamingkitchen.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class MenuItemController {

    @Autowired
    MenuItemService menuItemService;

    @PostMapping("/menuitem")
    public ResponseEntity<Object> addMenuItem(@RequestBody MenuItemDTO menuItemDTO)
    {
            return menuItemService.addMenuItem(menuItemDTO);
    }

    @PutMapping("/menuitem")
    public ResponseEntity<Object> updateMenuItem(@RequestBody MenuItemDTO menuItemDTO)
    {
        return menuItemService.updateMenuItem(menuItemDTO);
    }

    @GetMapping("/menuitem")
    public ResponseEntity<Object> getMenuItems()
    {

        return menuItemService.getMenuItems();
    }

    @DeleteMapping("/menuitem")
    public ResponseEntity<Object> deleteMenuItem(@RequestBody MenuItemDTO menuItemDTO)
    {
        return menuItemService.deleteMenuItems(menuItemDTO);
    }



}
