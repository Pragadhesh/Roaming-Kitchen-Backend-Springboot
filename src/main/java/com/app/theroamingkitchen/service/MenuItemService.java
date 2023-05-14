package com.app.theroamingkitchen.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.app.theroamingkitchen.DTO.MenuItemDTO;
import com.app.theroamingkitchen.models.FoodDish;
import com.app.theroamingkitchen.models.MenuItem;
import com.app.theroamingkitchen.repository.MenuItemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.square.Environment;
import com.squareup.square.SquareClient;
import com.squareup.square.api.CatalogApi;
import com.squareup.square.models.*;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class MenuItemService {

    @Autowired
    MenuItemRepository menuItemRepository;

    @Value("${aiaccesskey}")
    private String accesskey;

    @Value("${awsaccesskey}")
    private String awsaccesskey;

    @Value("${awssecretkey}")
    private String awssecretkey;

    @Value("${squareaccesstoken}")
    private String squareaccesstoken;

    public  ResponseEntity<Object> addMenuItem(MenuItemDTO menuitem)
    {
        log.info("Adding Menu Item");
      try {
          List<MenuItem> menuItems = menuItemRepository.findAll().
                  stream().filter(item -> item.getItemName().equals(menuitem.getItemName()))
                  .collect(Collectors.toList());
          if (menuItems.isEmpty()) {

              String name = menuitem.getItemName();

              if (name.split(" ").length == 1) { // Check if only one word
                  String outputString = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                  menuitem.setItemName(outputString);
              } else { // More than one word
                  String[] words = name.split(" ");
                  StringBuilder outputStringBuilder = new StringBuilder();
                  for (String word : words) {
                      String firstLetter = word.substring(0, 1);
                      String restOfWord = word.substring(1);
                      outputStringBuilder.append(firstLetter.toUpperCase()).append(restOfWord.toLowerCase()).append(" ");
                  }
                  String outputString = outputStringBuilder.toString().trim();
                  menuitem.setItemName(outputString);
              }

              log.info("Generating image for - "+menuitem.getItemName());
              // Set the request headers
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(MediaType.APPLICATION_JSON);
              headers.set("Authorization", "Bearer " + accesskey);

              String url = "https://api.openai.com/v1/images/generations";
              // Set the request body
              Map<String, Object> requestBody = new HashMap<>();
              requestBody.put("prompt","Ingredient -"+menuitem.getItemName());
              requestBody.put("n", 1);
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

              menuitem.setImageUrl(publicUrl);
              MenuItem finalmenu = new MenuItem(
                      menuitem.getItemName(), menuitem.getImageUrl(), menuitem.getAmount(),
                      menuitem.getUnit(),false
              );
              finalmenu.setLow(finalmenu.isLow());
              MenuItem mt = menuItemRepository.save(finalmenu);
              List<MenuItem> finalmenuItems = menuItemRepository.findAll();
              finalmenuItems.sort(Comparator.comparing(MenuItem::isLow).reversed());
              return new ResponseEntity<>(finalmenuItems, HttpStatus.OK);
          } else {
              return new ResponseEntity<>("Item already exists",HttpStatus.INTERNAL_SERVER_ERROR);
          }
      }
      catch (Exception e)
      {
          log.info(e.getMessage());
          return new ResponseEntity<>("Error in adding the item",HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }


    public ResponseEntity<Object> updateMenuItem(MenuItemDTO menuitemdto) {
        log.info("Updating Menu Item");
        try {
            Optional<MenuItem> menuItem = menuItemRepository.findById(menuitemdto.getId());
            MenuItem mt = menuItem.orElse(null);
            if (mt == null) {
                return new ResponseEntity<>("No Items found", HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                CompletableFuture<Void> apiCallFuture = CompletableFuture.completedFuture(null);
                if (menuitemdto.getRecipeLock() && !mt.isRecipeLock()) {
                    log.info("Locking all recipes");
                    Set<FoodDish> foodDishes = mt.getFoodDishes();
                    LinkedList<String> objectIds = new LinkedList<>();
                    for (FoodDish foodDish : foodDishes) {
                        String objectId = foodDish.getCatalogid();
                        objectIds.add(objectId);
                    }
                    if (objectIds.size() > 0) {
                        SquareClient client = new SquareClient.Builder()
                                .environment(Environment.SANDBOX)
                                .accessToken(squareaccesstoken)
                                .build();
                        // Create an instance of the Catalog API
                        CatalogApi catalogApi = client.getCatalogApi();
                        BatchRetrieveCatalogObjectsRequest body = new BatchRetrieveCatalogObjectsRequest.Builder(objectIds)
                                .build();
                        CompletableFuture<List<CatalogObject>> apiCall = catalogApi.batchRetrieveCatalogObjectsAsync(body)
                                .thenCompose(result -> {
                                    List<CatalogObject> catalogObjects = new ArrayList<>(result.getObjects());
                                    LinkedList<CatalogObject> updatedObjects = new LinkedList<>();
                                    for (CatalogObject catalogObject : catalogObjects) {
                                        CatalogItem item = catalogObject.getItemData();
                                        CatalogItem updateditem = new CatalogItem.Builder()
                                                .name(item.getName())
                                                .description(item.getDescription())
                                                .imageIds(item.getImageIds())
                                                .availableOnline(false)
                                                .variations(item.getVariations())
                                                .build();
                                        CatalogObject newcatalogObject = new CatalogObject.Builder("ITEM", catalogObject.getId())
                                                .itemData(updateditem)
                                                .version(catalogObject.getVersion())
                                                .build();
                                        updatedObjects.add(newcatalogObject);
                                    }
                                    CatalogObjectBatch catalogObjectBatch = new CatalogObjectBatch.Builder(updatedObjects)
                                            .build();
                                    LinkedList<CatalogObjectBatch> batches = new LinkedList<>();
                                    batches.add(catalogObjectBatch);
                                    BatchUpsertCatalogObjectsRequest newbody = new BatchUpsertCatalogObjectsRequest.Builder(UUID.randomUUID().toString(), batches)
                                            .build();
                                    return catalogApi.batchUpsertCatalogObjectsAsync(newbody)
                                            .thenApply(result1 -> catalogObjects);
                                });
                        apiCallFuture = apiCall.thenAccept(result -> {
                            System.out.println("Locked successfully");
                        }).exceptionally(exception -> {
                            System.out.println("Failed to make the request");
                            System.out.println(String.format("Exception: %s", exception.getMessage()));
                            return null;
                        });
                    }
                }
                apiCallFuture.join(); // Wait for the API call to complete
                mt.setAmount(menuitemdto.getAmount());
                mt.setLow(mt.isLow());
                mt.setRecipeLock(menuitemdto.getRecipeLock());
                MenuItem mts = menuItemRepository.save(mt);
                log.info("Updated menu item " + mts);
                List<MenuItem> finalmenuItems = menuItemRepository.findAll();
                return new ResponseEntity<>(finalmenuItems, HttpStatus.OK);
            }
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error in Updating the menu item",HttpStatus.INTERNAL_SERVER_ERROR);
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

    public ResponseEntity<Object> deleteMenuItems(MenuItemDTO menuitemdto) {
        log.info("Deleting menu Item");
        try {
            Optional<MenuItem> menuItem = menuItemRepository.findById(menuitemdto.getId());
            MenuItem mt = menuItem.orElse(null);
            if (mt == null)
            {
                return new ResponseEntity<>("No Items found",HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else
            {
                if (mt.getFoodDishes().size() > 0)
                {
                    return new ResponseEntity<>("Cannot delete menu Item as it is associated with food dish",HttpStatus.INTERNAL_SERVER_ERROR);
                }
                else
                {
                    menuItemRepository.deleteById(mt.getId());
                    List<MenuItem> finalmenuItems = menuItemRepository.findAll();
                    return new ResponseEntity<>(finalmenuItems, HttpStatus.OK);
                }
            }
        }
        catch (Exception e)
        {
            return new ResponseEntity<>("Error in removing the item",HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

}
