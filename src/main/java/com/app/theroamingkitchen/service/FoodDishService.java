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
import com.app.theroamingkitchen.models.UnitOfMeasurement;
import com.app.theroamingkitchen.repository.FoodDishRepository;
import com.app.theroamingkitchen.repository.MenuItemRepository;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.square.Environment;
import com.squareup.square.SquareClient;
import com.squareup.square.api.CatalogApi;
import com.squareup.square.models.BatchRetrieveCatalogObjectsRequest;
import com.squareup.square.models.CatalogObject;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Value("${squareaccesstoken}")
    private String squareaccesstoken;

    @Autowired
    MenuItemRepository menuItemRepository;

    @Autowired
    FoodDishRepository foodDishRepository;

    public ResponseEntity<Object> getMenuItems(FoodDishDTO foodDishDTO)
    {
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
                    " and their values in numeric or in decimal format in the form of following JSON format {\"ingredients\": [{\"name\": [ingredient name], \"quantity\": {\"name_of_the_unit\":[numbericvalue or decimal value]}}, ...]}" +
                    "Example:{\n" +
                    "  \"ingredients\": [\n" +
                    "    {\n" +
                    "      \"name\": \"Milk\",\n" +
                    "      \"quantity\": { \"cup\": 1 }\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"name\": \"Salt\",\n" +
                    "      \"quantity\": { \"teaspoon\": 0.25 }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}\n");
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
                // Regular expression pattern to match fractions
                Pattern fractionPattern = Pattern.compile("\\d+\\/\\d+");
                // Matcher to find and replace fractions
                Matcher matcher = fractionPattern.matcher(jsonContent);
                while (matcher.find()) {
                    String fraction = matcher.group();
                    String[] parts = fraction.split("/");
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    double decimalValue = numerator / denominator;
                    jsonContent = jsonContent.replace(fraction, Double.toString(decimalValue));
                }
                JsonNode jsonContentNode = objectMapper.readTree(jsonContent);
                log.info(String.valueOf(jsonContentNode));
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
                List<MenuItemResultDTO> results = new ArrayList<>();

                // iterate over the ingredients and create a MenuItemDTO object for each one
                List<List<CompletableFuture<MenuItemResultDTO>>> allImageGenerationFutures = new ArrayList<>();


                // iterate over the ingredients and create a MenuItemDTO object for each one
                int ingredientIndex = 0;
                for (int i=0;i<size;i++) {
                    List<CompletableFuture<MenuItemResultDTO>> imageGenerationFutures = new ArrayList<>();
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
                    UnitOfMeasurement finalunit = null;
                    if (quantity.startsWith("GR"))
                    {
                        finalunit=GRAM;
                    }
                    else if (quantity.startsWith("TE"))
                    {
                        finalunit=TEASPOON;
                    }
                    else if (quantity.startsWith("TA"))
                    {
                        finalunit=TABLESPOON;
                    }
                    else if (quantity.startsWith("LI"))
                    {
                        finalunit=LITER;
                    }
                    else if (quantity.startsWith("CU"))
                    {
                        finalunit=CUP;
                    }
                    else
                    {
                        finalunit=PIECE;
                    }

                    String dishName = null;
                    String name = ingredientNode.get("name").asText();
                    if (name.split(" ").length == 1) { // Check if only one word
                        String outputString = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                        dishName=outputString;
                    } else { // More than one word
                        String[] words = name.split(" ");
                        StringBuilder outputStringBuilder = new StringBuilder();
                        for (String word : words) {
                            String firstLetter = word.substring(0, 1);
                            String restOfWord = word.substring(1);
                            outputStringBuilder.append(firstLetter.toUpperCase()).append(restOfWord.toLowerCase()).append(" ");
                        }
                        String outputString = outputStringBuilder.toString().trim();
                        dishName=outputString;
                    }

                    String finalDishName1 = dishName;
                    OptionalInt matchIndex = IntStream.range(0, menuitems.size())
                            .filter(dish -> menuitems.get(dish).getItemName().equals(finalDishName1))
                            .findFirst();


                    if(matchIndex.isPresent())
                    {
                        System.out.println("Entered if due to match");
                        int index = matchIndex.getAsInt();
                        results.add(new MenuItemResultDTO(
                                        menuitems.get(index).getId(),
                                        finalDishName1,
                                        menuitems.get(index).getImageUrl(),
                                        new Double(value),
                                        finalunit,
                                        true,
                                        menuitems.get(index).isRecipeLock()
                                )
                        );

                    }
                    else {
                        int finalI = i;
                        String finalValue = value;
                        UnitOfMeasurement finalUnit = finalunit;
                        String finalDishName = dishName;
                        CompletableFuture<MenuItemResultDTO> imageGenerationFuture = CompletableFuture.supplyAsync(() -> {
                            try {
                                log.info("Generating image for food dish menu: " + finalDishName);

                                // Set the request headers
                                HttpHeaders headers1 = new HttpHeaders();
                                headers1.setContentType(MediaType.APPLICATION_JSON);
                                headers1.set("Authorization", "Bearer " + imageaccesskey);
                                String url1 = "https://api.openai.com/v1/images/generations";
                                // Set the request body
                                Map<String, Object> requestBody1 = new HashMap<>();
                                requestBody1.put("prompt", "Ingredient-" + finalDishName);
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
                                String s3Key = "images/" +finalDishName+System.currentTimeMillis() + ".jpg";

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

                                return new MenuItemResultDTO(
                                        (long) (finalI),
                                        finalDishName,
                                        publicUrl,
                                        new Double(finalValue),
                                        finalUnit,
                                        false,
                                        false
                                );
                            } catch (Exception e) {
                                // Handle exception if image generation fails
                                log.error("Error generating image for dish: " + finalDishName, e);
                                return null;
                            }
                        });
                        imageGenerationFutures.add(imageGenerationFuture);
                    }
                    allImageGenerationFutures.add(imageGenerationFutures);
                }
                // Wait for all image generation tasks to complete
                CompletableFuture<Void> allImageGenerationFuture = CompletableFuture.allOf(
                        allImageGenerationFutures.stream()
                                .flatMap(List::stream)
                                .toArray(CompletableFuture[]::new)
                );

                // Combine the results of all image generation tasks
                CompletableFuture<List<MenuItemResultDTO>> combinedImageGenerationFuture = allImageGenerationFuture.thenApplyAsync(v -> {
                    List<MenuItemResultDTO> combinedResults = new ArrayList<>();
                    for (List<CompletableFuture<MenuItemResultDTO>> futures : allImageGenerationFutures) {
                        List<MenuItemResultDTO> imageGenerationResults = futures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList());
                        combinedResults.addAll(imageGenerationResults);
                    }
                    return combinedResults;
                });

                try {
                    // Wait for all image generation tasks to complete and retrieve the results
                    List<MenuItemResultDTO> imageGenerationResults = combinedImageGenerationFuture.get();

                    // Handle the image generation results and return the response
                    if (imageGenerationResults != null) {
                        results.addAll(imageGenerationResults);
                    }
                    return new ResponseEntity<>(results, HttpStatus.OK);
                } catch (Exception e) {
                    log.error("Error in generating items", e);
                    return new ResponseEntity<>("Error in generating items", HttpStatus.INTERNAL_SERVER_ERROR);
                }

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

    public  ResponseEntity<Object> getAllFoodDish() {
        log.info("Fetching all Recipes");
        try
        {
            List<FoodDish> finalfooddish = foodDishRepository.findAll();
            return new ResponseEntity<>(finalfooddish, HttpStatus.OK);
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error in fetching the fooddish",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

