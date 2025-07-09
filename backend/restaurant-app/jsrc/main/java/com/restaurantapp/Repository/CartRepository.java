package com.restaurantapp.Repository;

import com.restaurantapp.Model.Cart;
import com.restaurantapp.Model.Dish;
import com.restaurantapp.Model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Singleton;
import java.util.*;

@Singleton
public class CartRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CartRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String cartTableName = System.getenv("cart_table");
    private final String reservationsTableName = System.getenv("booking_table");

    public CartRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    public Map<String, String> getReservationDetails(String reservationId) {
        LOG.info("Fetching reservation details for ID: {}", reservationId);

        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(reservationsTableName)
                .key(Map.of("reservationId", AttributeValue.builder().s(reservationId).build()))
                .build();

        try {
            Map<String, AttributeValue> item = dynamoDbClient.getItem(getRequest).item();

            if (item == null || item.isEmpty()) {
                LOG.warn("No reservation found with ID: {}", reservationId);
                return null;
            }

            Map<String, String> details = new HashMap<>();
            details.put("customerEmail", item.containsKey("customerEmail") ? item.get("customerEmail").s() : "");
            details.put("date", item.containsKey("date") ? item.get("date").s() : "");
            details.put("locationId", item.containsKey("locationId") ? item.get("locationId").s() : "");
            details.put("slotId", item.containsKey("slotId") ? item.get("slotId").s() : "");
            details.put("timeSlot", item.containsKey("timeSlot") ? item.get("timeSlot").s() : "");
            details.put("address", item.containsKey("address") ? item.get("address").s() : "");

            return details;
        } catch (Exception e) {
            LOG.error("Error fetching reservation details for ID: {}", reservationId, e);
            return null;
        }
    }

    public Cart getCartByCustomerEmail(String customerEmail) {
        LOG.info("Fetching cart for customer: {}", customerEmail);

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(cartTableName)
                .keyConditionExpression("customerEmail = :email")
                .expressionAttributeValues(Map.of(
                        ":email", AttributeValue.builder().s(customerEmail).build()
                ))
                .build();

        try {
            QueryResponse response = dynamoDbClient.query(queryRequest);

            if (response.items().isEmpty()) {
                LOG.info("No cart found for customer: {}", customerEmail);
                return null;
            }

            Map<String, AttributeValue> item = response.items().get(0);
            Cart cart = new Cart();
            cart.setId(item.containsKey("id") ? item.get("id").s() : "");
            cart.setCustomerEmail(customerEmail);
            cart.setReservationId(item.containsKey("reservationId") ? item.get("reservationId").s() : "");
            cart.setDate(item.containsKey("date") ? item.get("date").s() : "");
            cart.setTimeSlot(item.containsKey("timeSlot") ? item.get("timeSlot").s() : "");
            cart.setAddress(item.containsKey("address") ? item.get("address").s() : "");
            cart.setState(item.containsKey("state") ? item.get("state").s() : "");
            cart.setLocationId(item.containsKey("locationId") ? item.get("locationId").s() : "");

            // Parse order items
            if (item.containsKey("orderItems") && item.get("orderItems").l() != null) {
                List<OrderItem> orderItems = new ArrayList<>();
                for (AttributeValue orderItemAttr : item.get("orderItems").l()) {
                    Map<String, AttributeValue> orderItemMap = orderItemAttr.m();
                    OrderItem orderItem = new OrderItem();
                    orderItem.setDishId(orderItemMap.containsKey("dishId") ? orderItemMap.get("dishId").s() : "");
                    orderItem.setDishName(orderItemMap.containsKey("dishName") ? orderItemMap.get("dishName").s() : "");
                    orderItem.setDishImageUrl(orderItemMap.containsKey("dishImageUrl") ? orderItemMap.get("dishImageUrl").s() : "");
                    orderItem.setDishPrice(orderItemMap.containsKey("dishPrice") ? orderItemMap.get("dishPrice").s() : "");

                    if (orderItemMap.containsKey("orderQuantity")) {
                        try {
                            orderItem.setOrderQuantity(Integer.parseInt(orderItemMap.get("orderQuantity").n()));
                        } catch (NumberFormatException e) {
                            LOG.error("Error parsing order quantity", e);
                            orderItem.setOrderQuantity(1);
                        }
                    } else {
                        orderItem.setOrderQuantity(1);
                    }

                    orderItems.add(orderItem);
                }
                cart.setOrderItems(orderItems);
            } else {
                cart.setOrderItems(new ArrayList<>());
            }

            return cart;
        } catch (Exception e) {
            LOG.error("Error fetching cart for customer: {}", customerEmail, e);
            return null;
        }
    }

    public boolean addDishToCart(String customerEmail, String reservationId, Dish dish) {
        LOG.info("Adding dish {} to cart for customer {}", dish.getDishId(), customerEmail);

        // Get existing cart or create new one
        Cart cart = getCartByCustomerEmail(customerEmail);
        Map<String, String> reservationDetails = getReservationDetails(reservationId);

        if (reservationDetails == null) {
            LOG.error("Reservation not found: {}", reservationId);
            return false;
        }

        if (cart == null) {
            cart = new Cart();
            cart.setId(UUID.randomUUID().toString());
            cart.setCustomerEmail(customerEmail);
            cart.setReservationId(reservationId);
            cart.setDate(reservationDetails.get("date"));
            cart.setTimeSlot(reservationDetails.get("timeSlot"));
            cart.setAddress(reservationDetails.get("address"));
            cart.setState("ACTIVE");
            cart.setLocationId(reservationDetails.get("locationId"));
            cart.setOrderItems(new ArrayList<>());
        }

        // Check if dish already exists in cart
        List<OrderItem> orderItems = cart.getOrderItems();
        boolean dishFound = false;

        for (OrderItem item : orderItems) {
            if (item.getDishId().equals(dish.getDishId())) {
                item.setOrderQuantity(item.getOrderQuantity() + 1);
                dishFound = true;
                break;
            }
        }

        // Add new dish if not found
        if (!dishFound) {
            OrderItem newItem = new OrderItem();
            newItem.setDishId(dish.getDishId());
            newItem.setDishName(dish.getDishName());
            newItem.setDishImageUrl(dish.getImageUrl());
            newItem.setDishPrice(String.format("$%.2f", dish.getPrice()));
            newItem.setOrderQuantity(1);
            orderItems.add(newItem);
        }

        // Save cart to DynamoDB
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(cart.getId()).build());
            item.put("customerEmail", AttributeValue.builder().s(customerEmail).build());
            item.put("reservationId", AttributeValue.builder().s(reservationId).build());
            item.put("date", AttributeValue.builder().s(cart.getDate()).build());
            item.put("timeSlot", AttributeValue.builder().s(cart.getTimeSlot()).build());
            item.put("address", AttributeValue.builder().s(cart.getAddress()).build());
            item.put("state", AttributeValue.builder().s(cart.getState()).build());
            item.put("locationId", AttributeValue.builder().s(cart.getLocationId()).build());

            // Convert order items to AttributeValue
            List<AttributeValue> orderItemsAttr = new ArrayList<>();
            for (OrderItem orderItem : orderItems) {
                Map<String, AttributeValue> orderItemMap = new HashMap<>();
                orderItemMap.put("dishId", AttributeValue.builder().s(orderItem.getDishId()).build());
                orderItemMap.put("dishName", AttributeValue.builder().s(orderItem.getDishName()).build());
                orderItemMap.put("dishImageUrl", AttributeValue.builder().s(orderItem.getDishImageUrl()).build());
                orderItemMap.put("dishPrice", AttributeValue.builder().s(orderItem.getDishPrice()).build());
                orderItemMap.put("orderQuantity", AttributeValue.builder().n(String.valueOf(orderItem.getOrderQuantity())).build());

                orderItemsAttr.add(AttributeValue.builder().m(orderItemMap).build());
            }
            item.put("orderItems", AttributeValue.builder().l(orderItemsAttr).build());

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(cartTableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(putRequest);
            return true;
        } catch (Exception e) {
            LOG.error("Error saving cart for customer: {}", customerEmail, e);
            return false;
        }
    }
    /**
     * Update cart details in the database
     * @param cart The cart to update
     * @return true if successful, false otherwise
     */
    public boolean updateCart(Cart cart) {
        LOG.info("Updating cart in database for customer: {}", cart.getCustomerEmail());

        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("customerEmail", AttributeValue.builder().s(cart.getCustomerEmail()).build());
            item.put("id", AttributeValue.builder().s(cart.getId()).build());
            item.put("reservationId", AttributeValue.builder().s(cart.getReservationId()).build());

            // Handle optional fields
            if (cart.getDate() != null) {
                item.put("date", AttributeValue.builder().s(cart.getDate()).build());
            }

            if (cart.getTimeSlot() != null) {
                item.put("timeSlot", AttributeValue.builder().s(cart.getTimeSlot()).build());
            }

            if (cart.getAddress() != null) {
                item.put("address", AttributeValue.builder().s(cart.getAddress()).build());
            }

            if (cart.getState() != null) {
                item.put("state", AttributeValue.builder().s(cart.getState()).build());
            }

            if (cart.getLocationId() != null) {
                item.put("locationId", AttributeValue.builder().s(cart.getLocationId()).build());
            }

            // Convert order items to AttributeValue
            if (cart.getOrderItems() != null && !cart.getOrderItems().isEmpty()) {
                List<AttributeValue> orderItemsAttr = new ArrayList<>();
                for (OrderItem orderItem : cart.getOrderItems()) {
                    Map<String, AttributeValue> orderItemMap = new HashMap<>();
                    orderItemMap.put("dishId", AttributeValue.builder().s(orderItem.getDishId()).build());
                    orderItemMap.put("dishName", AttributeValue.builder().s(orderItem.getDishName()).build());
                    orderItemMap.put("dishImageUrl", AttributeValue.builder().s(orderItem.getDishImageUrl()).build());
                    orderItemMap.put("dishPrice", AttributeValue.builder().s(orderItem.getDishPrice()).build());
                    orderItemMap.put("orderQuantity", AttributeValue.builder().n(String.valueOf(orderItem.getOrderQuantity())).build());

                    orderItemsAttr.add(AttributeValue.builder().m(orderItemMap).build());
                }
                item.put("orderItems", AttributeValue.builder().l(orderItemsAttr).build());
            }

            PutItemRequest putRequest = PutItemRequest.builder()
                    .tableName(cartTableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(putRequest);
            LOG.info("Cart updated successfully");
            return true;
        } catch (Exception e) {
            LOG.error("Error updating cart for customer: {}", cart.getCustomerEmail(), e);
            return false;
        }
    }
    public boolean clearCart(String customerEmail) {
        LOG.info("Clearing cart for customer: {}", customerEmail);

        try {
            // First get the cart to confirm it exists
            Cart cart = getCartByCustomerEmail(customerEmail);
            if (cart == null) {
                LOG.warn("No cart found to clear for customer: {}", customerEmail);
                return true; // Already cleared
            }

            LOG.info("Found cart with ID {} to clear", cart.getId());

            // Delete the cart using both primary key components
            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(cartTableName)
                    .key(Map.of(
                            "customerEmail", AttributeValue.builder().s(customerEmail).build(),
                            "id", AttributeValue.builder().s(cart.getId()).build()
                    ))
                    .build();

            dynamoDbClient.deleteItem(deleteRequest);
            LOG.info("Cart deleted successfully");

            return true;
        } catch (Exception e) {
            LOG.error("Error clearing cart for customer: {}", customerEmail, e);
            e.printStackTrace();
            return false;
        }
    }
}