package com.app.theroamingkitchen.controller;

import com.app.theroamingkitchen.DTO.CreateOrderDTO;
import com.app.theroamingkitchen.DTO.FoodDishDTO;
import com.app.theroamingkitchen.DTO.UpdateDeliveryDTO;
import com.app.theroamingkitchen.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/orders")
    public ResponseEntity<Object> getAllActiveOrders()
    {
        return orderService.getActiveOrders();
    }

    @PostMapping("/imagedetails")
    public ResponseEntity<Object> fetchALlImageUrls(@RequestBody List<String> catalogids)
    {
        return orderService.getAllCatalogImages(catalogids);
    }

    @PostMapping("/updatedelivery")
    public ResponseEntity<Object> updateDelivery(@RequestBody UpdateDeliveryDTO orderdet)
    {
        return orderService.updateDelivery(orderdet);
    }






}
