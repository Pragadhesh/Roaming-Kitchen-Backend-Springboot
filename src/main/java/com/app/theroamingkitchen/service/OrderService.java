package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.CartItemDTO;
import com.app.theroamingkitchen.DTO.CreateOrderDTO;
import com.app.theroamingkitchen.DTO.UpdateDeliveryDTO;
import com.app.theroamingkitchen.models.FoodDish;
import com.app.theroamingkitchen.repository.FoodDishRepository;
import com.squareup.square.Environment;
import com.squareup.square.SquareClient;
import com.squareup.square.api.CatalogApi;
import com.squareup.square.api.CustomersApi;
import com.squareup.square.api.OrdersApi;
import com.squareup.square.api.PaymentsApi;
import com.squareup.square.models.*;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {
    @Value("${squareaccesstoken}")
    private String squareaccesstoken;

    @Autowired
    FoodDishRepository foodDishRepository;

    public ResponseEntity<Object> createOrder(CreateOrderDTO createOrderDTO)
    {
        log.info("Creating order");
        try
        {
            SquareClient client = new SquareClient.Builder()
                    .environment(Environment.SANDBOX)
                    .accessToken(squareaccesstoken)
                    .build();

            Address address = new Address.Builder()
                    .addressLine1(createOrderDTO.getAddress())
                    .build();

            CreateCustomerRequest body = new CreateCustomerRequest.Builder()
                    .givenName(createOrderDTO.getFirstname()+" "+createOrderDTO.getLastname())
                    .emailAddress(createOrderDTO.getEmail())
                    .phoneNumber(createOrderDTO.getPhone())
                    .address(address)
                    .build();

            CustomersApi customersApi = client.getCustomersApi();
            OrdersApi ordersApi = client.getOrdersApi();
            PaymentsApi paymentsApi = client.getPaymentsApi();

            CompletableFuture<CreateCustomerResponse> createCustomerFuture = customersApi.createCustomerAsync(body)
                    .thenApply(result -> {
                        return result;
                    })
                    .exceptionally(exception -> {
                        System.out.println("Failed to create customer");
                        System.out.println(String.format("Exception: %s", exception.getMessage()));
                        return null;
                    });

            CreateCustomerResponse customerResponse = createCustomerFuture.join();

            List<CartItemDTO> cartitems = createOrderDTO.getCartitems();

            LinkedList<OrderLineItem> lineItems = new LinkedList<>();

            for(int i =0;i<cartitems.size();i++)
            {
                OrderLineItem orderLineItem = new OrderLineItem.Builder(cartitems.get(i).getQuantity())
                        .catalogObjectId(cartitems.get(i).getId())
                        .build();
                lineItems.add(orderLineItem);
            }


            FulfillmentRecipient recipient = new FulfillmentRecipient.Builder()
                    .customerId(customerResponse.getCustomer().getId())
                    .build();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();

            FulfillmentDeliveryDetails deliveryDetails = new FulfillmentDeliveryDetails.Builder()
                    .recipient(recipient)
                    .deliverAt(dateFormat.format(date))
                    .build();

            Fulfillment fulfillment = new Fulfillment.Builder()
                    .type("DELIVERY")
                    .deliveryDetails(deliveryDetails)
                    .build();

            LinkedList<Fulfillment> fulfillments = new LinkedList<>();
            fulfillments.add(fulfillment);

            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("customerlatitude", createOrderDTO.getCustomerlatitude());
            metadata.put("customerlongitude", createOrderDTO.getCustomerlongitude());
            metadata.put("storelatitude", createOrderDTO.getStorelatitude());
            metadata.put("storelongitude", createOrderDTO.getStorelongitude());

            Order order = new Order.Builder("LEV2G795N4REP")
                    .customerId(customerResponse.getCustomer().getId())
                    .lineItems(lineItems)
                    .fulfillments(fulfillments)
                    .metadata(metadata)
                    .build();

            CreateOrderRequest orderbody = new CreateOrderRequest.Builder()
                    .order(order)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();

            CompletableFuture<CreateOrderResponse> createOrderFuture = ordersApi.createOrderAsync(orderbody)
                    .thenApply(result -> {
                        return result;
                    })
                    .exceptionally(exception -> {
                        System.out.println("Failed to Create order");
                        System.out.println(String.format("Exception: %s", exception.getMessage()));
                        return null;
                    });
            CreateOrderResponse orderResponse = createOrderFuture.join();


            Money amountMoney = new Money.Builder()
                    .amount(orderResponse.getOrder().getTotalMoney().getAmount())
                    .currency(orderResponse.getOrder().getTotalMoney().getCurrency())
                    .build();

            CreatePaymentRequest paymentbody = new CreatePaymentRequest.Builder("cnon:card-nonce-ok", UUID.randomUUID().toString())
                    .amountMoney(amountMoney)
                    .orderId(orderResponse.getOrder().getId())
                    .customerId(customerResponse.getCustomer().getId())
                    .build();

            CompletableFuture<CreatePaymentResponse> createpaymentFuture =
                    paymentsApi.createPaymentAsync(paymentbody)
                            .thenApply(result -> {
                                return result;
                            })
                            .exceptionally(exception -> {
                                System.out.println("Failed to make the request");
                                System.out.println(String.format("Exception: %s", exception.getMessage()));
                                return null;
                            });

            CreatePaymentResponse paymentResponse = createpaymentFuture.join();

            return new ResponseEntity<>(paymentResponse,HttpStatus.OK);

        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in creating Order", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> getActiveOrders()
    {
        try {
            log.info("Fetching all active orders");
            SquareClient client = new SquareClient.Builder()
                    .environment(Environment.SANDBOX)
                    .accessToken(squareaccesstoken)
                    .build();
            OrdersApi ordersApi = client.getOrdersApi();

            LinkedList<String> locationIds = new LinkedList<>();
            locationIds.add("LEV2G795N4REP");

            LinkedList<String> fulfillmentTypes = new LinkedList<>();
            fulfillmentTypes.add("DELIVERY");

            LinkedList<String> fulfillmentStates = new LinkedList<>();
            fulfillmentStates.add("PROPOSED");

            SearchOrdersFulfillmentFilter fulfillmentFilter = new SearchOrdersFulfillmentFilter.Builder()
                    .fulfillmentTypes(fulfillmentTypes)
                    .fulfillmentStates(fulfillmentStates)
                    .build();

            SearchOrdersFilter filter = new SearchOrdersFilter.Builder()
                    .fulfillmentFilter(fulfillmentFilter)
                    .build();

            SearchOrdersQuery query = new SearchOrdersQuery.Builder()
                    .filter(filter)
                    .build();

            SearchOrdersRequest body = new SearchOrdersRequest.Builder()
                    .locationIds(locationIds)
                    .query(query)
                    .build();

            CompletableFuture<SearchOrdersResponse> searchOrdersFuture = ordersApi.searchOrdersAsync(body)
                    .thenApply(result -> {
                        return result;
                    })
                    .exceptionally(exception -> {
                        System.out.println("Failed to make the request");
                        System.out.println(String.format("Exception: %s", exception.getMessage()));
                        return null;
                    });
            SearchOrdersResponse orders = searchOrdersFuture.get();
            List<Order> finalorders = orders.getOrders();

            List<Order> customerorders = new ArrayList<>();

            for (Order order : finalorders) {
                Long balance = order.getNetAmountDueMoney().getAmount();
                if (balance == 0)
                {
                    customerorders.add(order);
                }
            }
            return new ResponseEntity<>(customerorders,HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in getting active orders", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> getAllCatalogImages(List<String> dishes) {
        try {
            log.info("Fetching all image urls");
            List<FoodDish> foodDishes = foodDishRepository.findByDishNameIn(dishes);

            List<String> imageUrls = new ArrayList<>();
            for (String dishName : dishes) {
                for (FoodDish foodDish : foodDishes) {
                    if (dishName.equals(foodDish.getDishName()) && foodDish.getImageUrl() != null) {
                        imageUrls.add(foodDish.getImageUrl());
                    }
                }
            }



            return new ResponseEntity<>(imageUrls,HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in getting Image data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<Object> updateDelivery(UpdateDeliveryDTO orderdetails)
    {
        try {
            SquareClient client = new SquareClient.Builder()
                    .environment(Environment.SANDBOX)
                    .accessToken(squareaccesstoken)
                    .build();
            OrdersApi ordersApi = client.getOrdersApi();

            CompletableFuture<RetrieveOrderResponse> orderFuture = ordersApi.retrieveOrderAsync(orderdetails.getOrderid())
                    .thenApply(result -> {
                        return result;
                    })
                    .exceptionally(exception -> {
                        System.out.println("Failed to make the request");
                        System.out.println(String.format("Exception: %s", exception.getMessage()));
                        return null;
                    });

            RetrieveOrderResponse order = orderFuture.get();
            Order order1 = order.getOrder();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();


            FulfillmentDeliveryDetails deliveryDetails = new FulfillmentDeliveryDetails.Builder()
                    .recipient(order1.getFulfillments().get(0).getDeliveryDetails().getRecipient())
                    .deliverAt(order1.getFulfillments().get(0).getDeliveryDetails().getDeliverAt())
                    .inProgressAt(dateFormat.format(date))
                    .inProgressAt(dateFormat.format(date))
                    .readyAt(dateFormat.format(date))
                    .build();

            Fulfillment fulfillment = new Fulfillment.Builder()
                    .type("DELIVERY")
                    .uid(order1.getFulfillments().get(0).getUid())
                    .deliveryDetails(deliveryDetails)
                    .state("COMPLETED")
                    .build();

            List<Fulfillment> fulfillments = new ArrayList<>();
            fulfillments.add(fulfillment);

            Order order2 = new Order.Builder(order1.getLocationId())
                    .customerId(order1.getCustomerId())
                    .lineItems(order1.getLineItems())
                    .fulfillments(fulfillments)
                    .metadata(order1.getMetadata())
                    .version(order1.getVersion())
                    .build();

            UpdateOrderRequest body = new UpdateOrderRequest.Builder()
                    .order(order2)
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();

            CompletableFuture<UpdateOrderResponse> updateOrderFuture = ordersApi.updateOrderAsync(orderdetails.getOrderid(), body)
                    .thenApply(result -> {
                        return result;
                    })
                    .exceptionally(exception -> {
                        System.out.println("Failed to make the request");
                        System.out.println(String.format("Exception: %s", exception.getMessage()));
                        return null;
                    });

            UpdateOrderResponse upresponse = updateOrderFuture.get();

            return new ResponseEntity<>("Order updated successfully",HttpStatus.OK);
        }
        catch (Exception e) {
            e.printStackTrace();
            log.info(e.getMessage());
            return new ResponseEntity<>("Error in updating delivery details", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}
