package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.CatalogDTO;
import com.app.theroamingkitchen.DTO.ItemVariationDTO;
import com.squareup.square.Environment;
import com.squareup.square.SquareClient;
import com.squareup.square.api.CatalogApi;
import com.squareup.square.exceptions.ApiException;
import com.squareup.square.models.*;
import com.squareup.square.utilities.FileWrapper;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class CatalogService<FileUrlWrapper> {

    @Value("${squareaccesstoken}")
    private String squareaccesstoken;

    public <UrlWrapper> ResponseEntity<Object> createCatalogObject(CatalogDTO catalog) {
        log.info("Creating catalog object");
        try {
            SquareClient client = new SquareClient.Builder()
                    .environment(Environment.SANDBOX)
                    .accessToken(squareaccesstoken)
                    .build();

            String itemId = "#" + catalog.getDishName(); // the ID of the item
            String itemName = catalog.getDishName(); // the name of the item

            CatalogApi catalogApi = client.getCatalogApi();

            List<ItemVariationDTO> variationsDTO = catalog.getVariations();

            LinkedList<CatalogObject> variations = new LinkedList<>();
            for (ItemVariationDTO variation : variationsDTO) {
                Money priceMoney = new Money.Builder()
                        .amount(variation.getAmount())
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
                        // Create the item object
                        List<String> imageids = new ArrayList<>();
                        imageids.add(catalogImage.getId());
                        CatalogItem item = new CatalogItem.Builder()
                                .name(itemName)
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

            CatalogObject catalogObject = future.join(); // Wait for the async call to complete
            return new ResponseEntity<>(catalogObject, HttpStatus.OK);
        } catch (Exception e) {
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in creating catalog object", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}