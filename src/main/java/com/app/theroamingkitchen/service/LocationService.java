package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.LocationDTO;
import com.app.theroamingkitchen.models.Location;
import com.app.theroamingkitchen.repository.LocationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class LocationService {

    @Autowired
    LocationRepository locationRepository;

    public ResponseEntity<Object> updateLocation(LocationDTO locationDTO)
    {
        log.info("Updating location");
        try
        {
            List<Location> locations = locationRepository.findAll();
            if(locations.isEmpty())
            {
                Location location = locationRepository.save(
                        new Location(locationDTO.getLatitude(),locationDTO.getLongitude())
                );
                return new ResponseEntity<>(location, HttpStatus.OK);
            }
            else
            {
               Location loc = locations.get(0);
               loc.setLatitude(locationDTO.getLatitude());
               loc.setLongitude(locationDTO.getLongitude());
               locationRepository.save(loc);
               return new ResponseEntity<>(loc,HttpStatus.OK);
            }
        }
        catch (Exception e)
        {
            log.info("Entered exception");
            System.out.println(e);
            return new ResponseEntity<>("Error in updating location", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> getLocation()
    {
        log.info("Fetching location");
        try
        {
            List<Location> locations = locationRepository.findAll();
            if(locations.isEmpty())
            {
                Location location =
                        new Location(0L,37.7749,-122.4194);
                return new ResponseEntity<>(location, HttpStatus.OK);
            }
            else
            {
                Location loc = locations.get(0);
                return new ResponseEntity<>(loc,HttpStatus.OK);
            }
        }
        catch (Exception e)
        {
            log.info("Entered exception");
            System.out.println(e);
            return new ResponseEntity<>("Error in fetching location", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
