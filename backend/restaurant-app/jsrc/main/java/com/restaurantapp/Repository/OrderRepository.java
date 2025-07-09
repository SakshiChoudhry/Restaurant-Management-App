package com.restaurantapp.Repository;

import com.restaurantapp.Model.Cart;
import com.restaurantapp.Model.Order;
import com.restaurantapp.Model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import javax.inject.Singleton;
import java.util.*;

@Singleton
public class OrderRepository {
    private static final Logger LOG = LoggerFactory.getLogger(OrderRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String ordersTableName = System.getenv("order_table");

    public OrderRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    public String createOrderFromCart(Cart cart) {
        LOG.info("Creating order from cart for customer: {}", cart.getCustomerEmail());

        String orderId = UUID.randomUUID().toString();

        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("orderId", AttributeValue.builder().s(orderId).build());
            item.put("customerEmail", AttributeValue.builder().s(cart.getCustomerEmail()).build());

            // Ensure date is not null
            String date = cart.getDate() != null ? cart.getDate() : "";
            item.put("date", AttributeValue.builder().s(date).build());

            // Ensure locationId is not null
            String locationId = cart.getLocationId() != null ? cart.getLocationId() : "";
            item.put("locationId", AttributeValue.builder().s(locationId).build());

            // Ensure reservationId is not null
            String reservationId = cart.getReservationId() != null ? cart.getReservationId() : "";
            item.put("reservationId", AttributeValue.builder().s(reservationId).build());

            item.put("state", AttributeValue.builder().s("SUBMITTED").build());

            // Ensure timeSlot is not null
            String timeSlot = cart.getTimeSlot() != null ? cart.getTimeSlot() : "";
            item.put("timeSlot", AttributeValue.builder().s(timeSlot).build());

            // Ensure address is not null
            String address = cart.getAddress() != null ? cart.getAddress() : "";
            item.put("address", AttributeValue.builder().s(address).build());

            LOG.info("Saving order with date: {}, timeSlot: {}, address: {}", date, timeSlot, address);

            // Convert order items to AttributeValue
            List<AttributeValue> dishItemsAttr = new ArrayList<>();
            for (OrderItem orderItem : cart.getOrderItems()) {
                Map<String, AttributeValue> dishItemMap = new HashMap<>();
                dishItemMap.put("dishId", AttributeValue.builder().s(orderItem.getDishId()).build());
                dishItemMap.put("dishName", AttributeValue.builder().s(orderItem.getDishName()).build());
                dishItemMap.put("dishImageUrl", AttributeValue.builder().s(orderItem.getDishImageUrl()).build());
                dishItemMap.put("dishPrice", AttributeValue.builder().s(orderItem.getDishPrice()).build());
                dishItemMap.put("dishQuantity", AttributeValue.builder().n(String.valueOf(orderItem.getOrderQuantity())).build());

                dishItemsAttr.add(AttributeValue.builder().m(dishItemMap).build());
            }
            item.put("dishItems", AttributeValue.builder().l(dishItemsAttr).build());

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(ordersTableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(putRequest);
            return orderId;
        } catch (Exception e) {
            LOG.error("Error creating order for customer: {}", cart.getCustomerEmail(), e);
            return null;
        }
    }

    public Order getOrderById(String orderId) {
        LOG.info("Fetching order with ID: {}", orderId);

        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(ordersTableName)
                .key(Map.of("orderId", AttributeValue.builder().s(orderId).build()))  // Use orderId as per table schema
                .build();

        try {
            Map<String, AttributeValue> item = dynamoDbClient.getItem(getRequest).item();

            if (item == null || item.isEmpty()) {
                LOG.warn("No order found with ID: {}", orderId);
                return null;
            }

            Order order = new Order();
            order.setId(orderId);  // Set id (not orderId) in the response model
            order.setCustomerEmail(item.containsKey("customerEmail") ? item.get("customerEmail").s() : "");
            order.setDate(item.containsKey("date") ? item.get("date").s() : "");
            order.setLocationId(item.containsKey("locationId") ? item.get("locationId").s() : "");
            order.setReservationId(item.containsKey("reservationId") ? item.get("reservationId").s() : "");
            order.setState(item.containsKey("state") ? item.get("state").s() : "");
            order.setTimeSlot(item.containsKey("timeSlot") ? item.get("timeSlot").s() : "");
            order.setAddress(item.containsKey("address") ? item.get("address").s() : "");

            // Parse dish items
            List<Order.DishItem> dishItems = new ArrayList<>();
            if (item.containsKey("dishItems") && item.get("dishItems").l() != null) {
                for (AttributeValue dishItemAttr : item.get("dishItems").l()) {
                    Map<String, AttributeValue> dishItemMap = dishItemAttr.m();
                    Order.DishItem dishItem = new Order.DishItem();
                    dishItem.setDishId(dishItemMap.containsKey("dishId") ? dishItemMap.get("dishId").s() : "");
                    dishItem.setDishName(dishItemMap.containsKey("dishName") ? dishItemMap.get("dishName").s() : "");
                    dishItem.setDishImageUrl(dishItemMap.containsKey("dishImageUrl") ? dishItemMap.get("dishImageUrl").s() : "");
                    dishItem.setDishPrice(dishItemMap.containsKey("dishPrice") ? dishItemMap.get("dishPrice").s() : "");

                    if (dishItemMap.containsKey("dishQuantity")) {
                        try {
                            dishItem.setDishQuantity(Integer.parseInt(dishItemMap.get("dishQuantity").n()));
                        } catch (NumberFormatException e) {
                            LOG.error("Error parsing dish quantity", e);
                            dishItem.setDishQuantity(1);
                        }
                    } else {
                        dishItem.setDishQuantity(1);
                    }

                    dishItems.add(dishItem);
                }
            }
            order.setDishItems(dishItems);

            return order;
        } catch (Exception e) {
            LOG.error("Error fetching order with ID: {}", orderId, e);
            return null;
        }
    }
}