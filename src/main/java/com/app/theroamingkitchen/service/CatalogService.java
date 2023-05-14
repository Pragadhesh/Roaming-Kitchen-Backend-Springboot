package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.*;
import com.app.theroamingkitchen.models.FoodDish;
import com.app.theroamingkitchen.models.MenuItem;
import com.app.theroamingkitchen.models.MenuItemUsage;
import com.app.theroamingkitchen.repository.FoodDishRepository;
import com.app.theroamingkitchen.repository.MenuItemRepository;
import com.app.theroamingkitchen.repository.MenuItemUsageRepository;
import com.squareup.square.Environment;
import com.squareup.square.SquareClient;
import com.squareup.square.api.CatalogApi;
import com.squareup.square.exceptions.ApiException;
import com.squareup.square.models.*;
import com.squareup.square.utilities.FileWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class CatalogService<FileUrlWrapper> {

    @Value("${squareaccesstoken}")
    private String squareaccesstoken;

    @Autowired
    MenuItemRepository menuItemRepository;

    @Autowired
    MenuItemUsageRepository menuItemUsageRepository;

    @Autowired
    FoodDishRepository foodDishRepository;

    public ResponseEntity<Object> createCatalogObject(CatalogDTO catalog) {
        log.info("Creating catalog object");
        try {
            SquareClient client = new SquareClient.Builder()
                    .environment(Environment.SANDBOX)
                    .accessToken(squareaccesstoken)
                    .build();

            String itemId = "#" + catalog.getDishName(); // the ID of the item
            String itemName = catalog.getDishName(); // the name of the item
            String itemDescription = catalog.getDescription(); // description of the item

            CatalogApi catalogApi = client.getCatalogApi();

            List<ItemVariationDTO> variationsDTO = catalog.getVariations();

            LinkedList<CatalogObject> variations = new LinkedList<>();
            for (ItemVariationDTO variation : variationsDTO) {
                Money priceMoney = new Money.Builder()
                        .amount(variation.getAmount()*100)
                        .currency("USD")
                        .build();

                CatalogItemVariation itemVariationData = new CatalogItemVariation.Builder()
                        .itemId(itemId)
                        .name(variation.getName())
                        .pricingType("FIXED_PRICING")
                        .priceMoney(priceMoney)
                        .build();

                CatalogObject variationObject = new CatalogObject.Builder("ITEM_VARIATION", "#" + variation.getName())
                        .itemVariationData(itemVariationData)
                        .build();

                variations.add(variationObject);
            }

            // Create a URL object for the public image URL
            URL url = new URL(catalog.getImageUrl());

            // Open a connection to the URL
            URLConnection connection = url.openConnection();

            // Get the image data as an input stream
            InputStream inputStream = connection.getInputStream();

            // Read the contents of the input stream into a byte array
            byte[] imageData = inputStream.readAllBytes();

            // Create a temporary file and write the byte array to it
            File tempFile = File.createTempFile("image", ".jpg");
            Files.write(tempFile.toPath(), imageData);

            // Wrap the file in a FileWrapper object
            FileWrapper imageFile = new FileWrapper(tempFile);

            CatalogImage imageDatas = new CatalogImage.Builder()
                    .build();

            CatalogObject image = new CatalogObject.Builder("IMAGE", itemId)
                    .imageData(imageDatas)
                    .build();

            CreateCatalogImageRequest request = new CreateCatalogImageRequest.Builder(UUID.randomUUID().toString(), image)
                    .build();


            CompletableFuture<CatalogObject> future = catalogApi.createCatalogImageAsync(request, imageFile)
                    .thenApply(result -> {
                        System.out.println("Created catalog image");
                        CatalogObject catalogImage = result.getImage();
                        return catalogImage;
                    })
                    .thenApply(catalogImage -> {
                        List<String> imageids = new ArrayList<>();
                        imageids.add(catalogImage.getId());
                        CatalogItem item = new CatalogItem.Builder()
                                .name(itemName)
                                .description(itemDescription)
                                .imageIds(imageids)
                                .availableOnline(false)
                                .variations(variations)
                                .build();

                        // Create the catalog object
                        CatalogObject catalogObject = new CatalogObject.Builder("ITEM", itemId)
                                .itemData(item)
                                .build();

                        // Create upsert catalog object request
                        UpsertCatalogObjectRequest body = new UpsertCatalogObjectRequest.Builder(UUID.randomUUID().toString(), catalogObject).build();

                        // Upsert the catalog object
                        UpsertCatalogObjectResponse response = null;
                        try {
                            response = catalogApi.upsertCatalogObject(body);
                        } catch (ApiException e) {
                            throw new RuntimeException(e);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        // Return the upserted catalog object
                        return response.getCatalogObject();
                    })
                    .exceptionally(exception -> {
                        System.out.println("Failed to make the request");
                        System.out.println(String.format("Exception: %s", exception.getMessage()));
                        // code to handle exceptions
                        return null;
                    });

            CatalogObject catalogObject = future.join();
            String catalogObjectId = catalogObject.getId();
            FoodDish foodDish = foodDishRepository.save(new FoodDish(catalog.getDishName(),catalogObjectId,catalog.getImageUrl()));

            List<MenuItemResultDTO> menuitems = catalog.getIngredients();
            Set<MenuItem> finalMenuItems = new HashSet<>();

            for(int i=0; i<menuitems.size(); i++) {
                MenuItemResultDTO mt = menuitems.get(i);
                System.out.println(mt);
                System.out.println(mt.getStatus());
                if(mt.getStatus())
                {
                    Optional<MenuItem> opmt = menuItemRepository.findById(mt.getId());
                    MenuItem mts = opmt.orElse(null);
                    if(mts != null) {
                        MenuItemUsage usage = new MenuItemUsage(mts, foodDish, mt.getAmount());
                        MenuItemUsage menuusage = menuItemUsageRepository.save(usage);

                        Set<FoodDish> fdish = mts.getFoodDishes();
                        fdish.add(foodDish);
                        mts.setFoodDishes(fdish);
                        MenuItem finalmenuitem =menuItemRepository.save(mts);
                        finalMenuItems.add(finalmenuitem);
                    }
                    else {
                        log.info("Cant find the menu item specified");
                    }
                }
                else
                {
                    MenuItem mts = new MenuItem(mt.getItemName(),mt.getImageUrl(),
                            0.0,mt.getUnit(),false);
                    Set<FoodDish> fdish = mts.getFoodDishes();
                    fdish.add(foodDish);
                    mts.setFoodDishes(fdish);
                    MenuItem initial = menuItemRepository.save(mts);

                    MenuItemUsage usage = new MenuItemUsage(initial, foodDish, mt.getAmount());
                    MenuItemUsage menuusage = menuItemUsageRepository.save(usage);

                    finalMenuItems.add(initial);
                }
            }
            foodDish.setMenuItems(finalMenuItems);
            FoodDish finalfooddish = foodDishRepository.save(foodDish);
            return new ResponseEntity<>(finalfooddish, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in creating catalog object", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> fetchCatalogItemDetails(FoodDishDTO fooddishdto)
    {
        try
        {
            FoodDish foodDish = foodDishRepository.findFirstByCatalogid(fooddishdto.getCatalogId());
            if (foodDish == null)
            {
                return new ResponseEntity<>("No fooddish found",HttpStatus.INTERNAL_SERVER_ERROR);
            }
            else
            {
                UpdateCatalogItemDTO catalogitem = new UpdateCatalogItemDTO();
                catalogitem.setFoodDish(foodDish);
                catalogitem.setMenuitems(foodDish.getMenuItems());
                SquareClient client = new SquareClient.Builder()
                        .environment(Environment.SANDBOX)
                        .accessToken(squareaccesstoken)
                        .build();
                // Create an instance of the Catalog API
                CatalogApi catalogApi = client.getCatalogApi();

                catalogApi.retrieveCatalogObjectAsync(fooddishdto.getCatalogId(), null, null)
                        .thenAccept(result -> {
                            // Extract the catalog item data
                            CatalogObject catalogObject = result.getObject();
                            if (catalogObject.getType().equals("ITEM")) {
                                CatalogItem itemObject = catalogObject.getItemData();
                                catalogitem.setAvailability(itemObject.getAvailableOnline());

                            }
                        })
                        .exceptionally(exception -> {
                            System.out.println("Failed to make the request");
                            System.out.println(String.format("Exception: %s", exception.getMessage()));
                            return null;
                        })
                        .join();
                return new ResponseEntity<>(catalogitem,HttpStatus.OK);

            }

        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in fetching object details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }





}
