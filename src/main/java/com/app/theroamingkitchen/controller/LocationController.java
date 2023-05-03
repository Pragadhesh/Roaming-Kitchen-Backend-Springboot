package com.app.theroamingkitchen.controller;

import com.app.theroamingkitchen.DTO.LocationDTO;
import com.app.theroamingkitchen.DTO.MenuItemDTO;
import com.app.theroamingkitchen.service.LocationService;
import com.app.theroamingkitchen.service.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api")
public class LocationController {

    @Autowired
    LocationService locationService;

    @PutMapping("/locations")
    public ResponseEntity<Object> updateLocation(@RequestBody LocationDTO locationDTO)
    {
        return locationService.updateLocation(locationDTO);
    }

    @GetMapping("/locations")
    public ResponseEntity<Object> getLocations()
    {
        return locationService.getLocation();
    }

}
