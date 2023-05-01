package com.app.theroamingkitchen.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.app.theroamingkitchen.DTO.DetailsDTO;
import com.app.theroamingkitchen.DTO.FoodDishDTO;
import com.app.theroamingkitchen.DTO.MenuItemResultDTO;
import com.app.theroamingkitchen.models.FoodDish;
import com.app.theroamingkitchen.models.MenuItem;
import com.app.theroamingkitchen.repository.FoodDishRepository;
import com.app.theroamingkitchen.repository.MenuItemRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.app.theroamingkitchen.models.UnitOfMeasurement.*;

@Service
@Slf4j
public class FoodDishService {

    @Value("${aiaccesskey}")
    private String accesskey;

    @Value("${imageaccesskey}")
    private String imageaccesskey;

    @Value("${awsaccesskey}")
    private String awsaccesskey;

    @Value("${awssecretkey}")
    private String awssecretkey;

    @Autowired
    MenuItemRepository menuItemRepository;

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
                    "in the following JSON format {\"ingredients\": [{\"name\": [ingredient name], \"quantity\": {\"name_of_the_unit\":[value]}}, ...]}" +
                    "Example:{}");
            messagesList.add(messageMap);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages",messagesList);


            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            JsonNode messageNode = rootNode.get("choices").get(0).get("message");
            String content = messageNode.get("content").asText();
            System.out.println(content);

            // Find the index of the first '{' character in the string
            int startIndex = content.indexOf("{");

            // If the string contains a '{' character, extract the JSON substring
            if (startIndex >= 0) {
                String jsonContent = content.substring(startIndex);
                JsonNode jsonContentNode = objectMapper.readTree(jsonContent);
                log.info(String.valueOf(jsonContentNode));
            // get the "ingredients" array from the JsonNode
            JsonNode ingredientsNode = jsonContentNode.get("ingredients");

            int size;
            if(ingredientsNode.size() < 11)
            {
                size = ingredientsNode.size();
            }
            else {
                size = 10;
            }

            // Add all menu items to a list for verification
            List<MenuItem> menuitems = menuItemRepository.findAll();
            List<DetailsDTO> detailsDTO = new ArrayList<>();
            menuitems.forEach(item ->
                    detailsDTO.add(new DetailsDTO(item.getItemName(), item.getUnit()))
            );
            System.out.println(detailsDTO);
            List<MenuItemResultDTO> results = new ArrayList<>();

            // iterate over the ingredients and create a MenuItemDTO object for each one
            for (int i = 0; i < size; i++) {
                System.out.println("Entered for with index "+i);
                JsonNode ingredientNode = ingredientsNode.get(i);
                JsonNode quantityNode = ingredientNode.get("quantity");
                String quantity = quantityNode.fieldNames().next();
                String value = quantityNode.get(quantity).toString();

                String result;
                if (value.matches("\\d+")) { // whole number
                    int intValue = Integer.parseInt(value);
                    // add logic to handle whole number input
                    result = String.valueOf(intValue);
                }
                else if (value.matches("\\d+\\.\\d+")) { // decimal value
                    double decimalValue = Double.parseDouble(value);
                    // add logic to handle decimal input
                    result = String.valueOf(decimalValue);
                }
                else if (value.matches("\\d+/\\d+")) { // fraction
                    String[] parts = value.split("/");
                    double decimalValue = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                    // add logic to handle fraction input
                    result = String.valueOf(decimalValue);
                }
                else  { // range
                    String[] parts = value.split("-");
                    double rangeStart = Double.parseDouble(parts[0]);
                    double rangeEnd = Double.parseDouble(parts[1]);
                    double averageValue = (rangeStart + rangeEnd) / 2.0;
                    // add logic to handle range input
                    result = String.valueOf(averageValue);
                }
                value = result;
                DetailsDTO dts = new DetailsDTO();
                if (quantity.startsWith("GR"))
                {
                    dts.setUnit(GRAM);
                }
                else if (quantity.startsWith("TE"))
                {
                    dts.setUnit(TEASPOON);
                }
                else if (quantity.startsWith("TA"))
                {
                    dts.setUnit(TABLESPOON);
                }
                else if (quantity.startsWith("LI"))
                {
                    dts.setUnit(LITER);
                }
                else if (quantity.startsWith("CU"))
                {
                    dts.setUnit(CUP);
                }
                else
                {
                    dts.setUnit(PIECE);
                }

                log.info("Generating name");
                String name = ingredientNode.get("name").asText();
                if (name.split(" ").length == 1) { // Check if only one word
                    String outputString = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                    dts.setDishName(outputString);
                } else { // More than one word
                    String[] words = name.split(" ");
                    StringBuilder outputStringBuilder = new StringBuilder();
                    for (String word : words) {
                        String firstLetter = word.substring(0, 1);
                        String restOfWord = word.substring(1);
                        outputStringBuilder.append(firstLetter.toUpperCase()).append(restOfWord.toLowerCase()).append(" ");
                    }
                    String outputString = outputStringBuilder.toString().trim();
                    dts.setDishName(outputString);
                }
                System.out.println("DTS Generated");
                System.out.println(dts);
                if(detailsDTO.contains(dts))
                {
                    System.out.println("Entered if due to match");
                   results.add(new MenuItemResultDTO(
                            menuitems.get(detailsDTO.indexOf(dts)).getId(),
                            dts.getDishName(),
                            menuitems.get(detailsDTO.indexOf(dts)).getImageUrl(),
                            value,
                            dts.getUnit(),
                            true
                    )
                   );

                }
                else
                {
                    log.info("Generating image for food dish menu : "+dts.getDishName());
                    // Set the request headers
                    HttpHeaders headers1 = new HttpHeaders();
                    headers1.setContentType(MediaType.APPLICATION_JSON);
                    if(i%2 ==0) {
                        headers1.set("Authorization", "Bearer " + accesskey);
                    }
                    else {
                        headers1.set("Authorization","Bearer "+ imageaccesskey);
                    }
                    String url1 = "https://api.openai.com/v1/images/generations";
                    // Set the request body
                    Map<String, Object> requestBody1 = new HashMap<>();
                    requestBody1.put("prompt", "Ingredient-"+dts.getDishName());
                    requestBody1.put("n", 1);
                    requestBody1.put("size", "1024x1024");
                    HttpEntity<Map<String, Object>> request1 = new HttpEntity<>(requestBody1, headers1);
                    RestTemplate restTemplate1 = new RestTemplate();
                    ResponseEntity<String> response1 = restTemplate1.postForEntity(url1, request1, String.class);
                    ObjectMapper objectMapper1 = new ObjectMapper();

                    JsonNode jsonNode = objectMapper1.readTree(response1.getBody());
                    String image = jsonNode.get("data").get(0).get("url").asText();

                    URL imageUrl = new URL(image);

                    BasicAWSCredentials credentials = new BasicAWSCredentials(awsaccesskey, awssecretkey);

                    AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                            .withCredentials(new AWSStaticCredentialsProvider(credentials))
                            .withRegion("ap-south-1")
                            .build();

                    // Generate a unique key for the object in S3
                    String s3Key = "images/" + System.currentTimeMillis() + ".jpg";

                    File imageFile = File.createTempFile("image-", ".jpg");
                    try (InputStream in = imageUrl.openStream(); OutputStream out = new FileOutputStream(imageFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    // Upload the image to S3
                    PutObjectRequest putRequest = new PutObjectRequest("theroamingkitchen", s3Key, imageFile)
                            .withCannedAcl(CannedAccessControlList.PublicRead);
                    PutObjectResult putResult = s3Client.putObject(putRequest);

                    // Get the public URL of the image in S3
                    String publicUrl = s3Client.getUrl("theroamingkitchen", s3Key).toString();

                    results.add(new MenuItemResultDTO(
                            (long) (i),
                            dts.getDishName(),
                            publicUrl,
                            value,
                            dts.getUnit(),
                            false
                    )
                    );
                }
            }
                return  new ResponseEntity<>(results,HttpStatus.OK);
            } else {
                throw new Exception("Recipe not found");
            }

        }
        catch (Exception e)
        {
            log.info("Entered exception");
            System.out.println(e);
            return new ResponseEntity<>("Error in generating items", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> getImagesforDescription(FoodDishDTO foodDishDTO)
    {
        log.info("Generating catalog image for "+ foodDishDTO.getDishName());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accesskey);

            String url = "https://api.openai.com/v1/images/generations";
            // Set the request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt",foodDishDTO.getDishName());
            requestBody.put("n", 3);
            requestBody.put("size", "1024x1024");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String image = jsonNode.get("data").get(0).get("url").asText();

            URL imageUrl = new URL(image);

            BasicAWSCredentials credentials = new BasicAWSCredentials(awsaccesskey, awssecretkey);

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion("ap-south-1")
                    .build();

            // Generate a unique key for the object in S3
            String s3Key = "images/" + System.currentTimeMillis() + ".jpg";

            File imageFile = File.createTempFile("image-", ".jpg");
            try (InputStream in = imageUrl.openStream(); OutputStream out = new FileOutputStream(imageFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Upload the image to S3
            PutObjectRequest putRequest = new PutObjectRequest("theroamingkitchen", s3Key, imageFile)
                    .withCannedAcl(CannedAccessControlList.PublicRead);
            PutObjectResult putResult = s3Client.putObject(putRequest);

            // Get the public URL of the image in S3
            String publicUrl = s3Client.getUrl("theroamingkitchen", s3Key).toString();

            return new ResponseEntity<>(publicUrl,HttpStatus.OK);
        }
        catch (Exception e)
        {
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in Generating image",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

