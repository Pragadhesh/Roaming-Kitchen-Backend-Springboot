package com.app.theroamingkitchen.service;

import com.app.theroamingkitchen.DTO.CartItemDTO;
import com.app.theroamingkitchen.DTO.CreateOrderDTO;
import com.squareup.square.Environment;
import com.squareup.square.SquareClient;
import com.squareup.square.api.CatalogApi;
import com.squareup.square.api.CustomersApi;
import com.squareup.square.api.OrdersApi;
import com.squareup.square.api.PaymentsApi;
import com.squareup.square.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderService {
    @Value("${squareaccesstoken}")
    private String squareaccesstoken;

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
            metadata.put("latitude", createOrderDTO.getCustomerlatitude());
            metadata.put("longitude", createOrderDTO.getCustomerlongitude());

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


}
