package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.FoodDishDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class FoodDishService {

    @Value("${aiaccesskey}")
    private String accesskey;

    public ResponseEntity<Object> getMenuItems(FoodDishDTO foodDishDTO)
    {
        log.info("Getting menu items");
        try
        {
            log.info("Generating menu items for "+foodDishDTO.getDishName() );

            RestTemplate restTemplate = new RestTemplate();
            // Set the request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accesskey);

            String url = "https://api.openai.com/v1/chat/completions";

            List<Map<String, String>> messagesList = new ArrayList<>();

            Map<String, String> messageMap = new HashMap<>();
            messageMap.put("role", "user");
            messageMap.put("content", "Send me the main ingredients for making "+foodDishDTO.getDishName()+"in json in the following standard units of measurements-PIECE, GRAM, TEASPOON, TABLESPOON, CUP, LITER" +
                    "in the following JSON format {'ingredients': [{'name': [ingredient name], 'quantity': [quantity]}, ...]}");
            messagesList.add(messageMap);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages",messagesList);


            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            JsonNode messageNode = rootNode.get("choices").get(0).get("message");
            String content = messageNode.get("content").asText();
            JsonNode jsonContentNode = objectMapper.readTree(content);
            return  new ResponseEntity<>(jsonContentNode,HttpStatus.OK);
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error in generating items", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

