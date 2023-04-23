package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.MenuItemDTO;
import com.app.theroamingkitchen.models.MenuItem;
import com.app.theroamingkitchen.repository.MenuItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MenuItemService {

    @Autowired
    MenuItemRepository menuItemRepository;

    @Value("${aiaccesskey}")
    private String accesskey;

    public  ResponseEntity<Object> addMenuItem(MenuItemDTO menuitem)
    {
        log.info("Adding Menu Item");
      try {
          List<MenuItem> menuItems = menuItemRepository.findAll().
                  stream().filter(item -> item.getItemName().equals(menuitem.getItemName()))
                  .collect(Collectors.toList());
          if (menuItems.isEmpty()) {

              log.info("Generating image for"+menuitem.getItemName());
              // Set the request headers
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);
              headers.set("Authorization", "Bearer " + accesskey);

              String url = "https://api.openai.com/v1/images/generations";
              // Set the request body
              Map<String, Object> requestBody = new HashMap<>();
              requestBody.put("prompt", menuitem.getItemName());
              requestBody.put("n", 1);
              requestBody.put("size", "1024x1024");

              HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
              RestTemplate restTemplate = new RestTemplate();
              ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
              ObjectMapper objectMapper = new ObjectMapper();
              JsonNode jsonNode = objectMapper.readTree(response.getBody());
              String imageUrl = jsonNode.get("data").get(0).get("url").asText();
              menuitem.setImageUrl(imageUrl);
              log.info("Adding menu Item:" + menuitem);
              MenuItem mt = menuItemRepository.save(new MenuItem(
                              menuitem.getItemName(), menuitem.getImageUrl(), menuitem.getAmount(),
                              menuitem.getUnit()
                      )
              );
              log.info("Added menu item "+ mt);
              List<MenuItem> finalmenuItems = menuItemRepository.findAll();
              return new ResponseEntity<>(finalmenuItems, HttpStatus.OK);
          } else {
              return new ResponseEntity<>("Item already exists",HttpStatus.INTERNAL_SERVER_ERROR);
          }
      }
      catch (Exception e)
      {
          return new ResponseEntity<>("Error in adding the item",HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    public  ResponseEntity<Object> updateMenuItem(MenuItemDTO menuitemdto) {
        log.info("Updating Menu Item");
        try
        {
            Optional<MenuItem> menuItem = menuItemRepository.findById(menuitemdto.getId());
            MenuItem mt = menuItem.orElse(null);
            if (mt == null)
            {
                return new ResponseEntity<>("No Items found",HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else
            {
                mt.setAmount(menuitemdto.getAmount());
                MenuItem mts = menuItemRepository.save(mt);
                log.info("Updated menu item "+ mts);
                List<MenuItem> finalmenuItems = menuItemRepository.findAll();
                return new ResponseEntity<>(finalmenuItems, HttpStatus.OK);
            }
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error in updating the item",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public  ResponseEntity<Object> getMenuItems() {
        log.info("Fetching all Menu Items");
        try
        {
            List<MenuItem> finalmenuItems = menuItemRepository.findAll();
            return new ResponseEntity<>(finalmenuItems, HttpStatus.OK);
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error in fetching the items",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
