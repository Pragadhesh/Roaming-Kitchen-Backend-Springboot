package com.app.theroamingkitchen.controller;

import com.app.theroamingkitchen.DTO.CreateOrderDTO;
import com.app.theroamingkitchen.DTO.FoodDishDTO;
import com.app.theroamingkitchen.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<Object> getMenuItems(@RequestBody CreateOrderDTO createOrderDTO)
    {
        return orderService.createOrder(createOrderDTO);
    }
}
