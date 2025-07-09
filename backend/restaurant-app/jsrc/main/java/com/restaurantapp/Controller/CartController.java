package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Model.Cart;
import com.restaurantapp.Model.Order;
import com.restaurantapp.Service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CartController {
    private static final Logger LOG = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;
    private final ObjectMapper objectMapper;

    @Inject
    public CartController(CartService cartService, ObjectMapper objectMapper) {
        this.cartService = cartService;
        this.objectMapper = objectMapper;
    }

    public APIGatewayProxyResponseEvent addDishToCart(String reservationId, String dishId, Map<String, Object> claims) {
        try {
            LOG.info("Adding dish {} to cart for reservation {}", dishId, reservationId);

            // Get customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isBlank()) {
                LOG.error("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated");
            }

            boolean success = cartService.addDishToCart(customerEmail, reservationId, dishId);
            if (success) {
                return ApiResponse.success(Map.of("message", "Dish has been placed to cart"));
            } else {
                return ApiResponse.error("Failed to add dish to cart. Dish may not be available at the selected location.");
            }
        } catch (Exception e) {
            LOG.error("Error adding dish to cart", e);
            return ApiResponse.serverError("Internal server error: " + e.getMessage());
        }
    }

    public APIGatewayProxyResponseEvent getCart(Map<String, Object> claims) {
        try {
            LOG.info("Fetching cart for user");

            // Get customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isBlank()) {
                LOG.error("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated");
            }

            Cart cart = cartService.getCart(customerEmail);
            if (cart == null) {
                LOG.info("No cart found, returning empty content");
                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("content", Collections.emptyList());
                return ApiResponse.success(responseBody);
            }

            LOG.info("Returning cart with {} items",
                    cart.getOrderItems() != null ? cart.getOrderItems().size() : 0);

            // Format the response according to the expected structure
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("content", Collections.singletonList(cart));
            return ApiResponse.success(responseBody);
        } catch (Exception e) {
            LOG.error("Error fetching cart", e);
            return ApiResponse.serverError("Internal server error: " + e.getMessage());
        }
    }

    public APIGatewayProxyResponseEvent submitOrder(String body, Map<String, Object> claims) {
        try {
            LOG.info("Submitting order");

            // Get customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isBlank()) {
                LOG.error("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated");
            }

            // Add debug logging
            LOG.info("Request body: {}", body);

            // Parse the request body if it contains address and timeSlot
            if (body != null && !body.isBlank()) {
                try {
                    Map<String, Object> requestMap = objectMapper.readValue(body, Map.class);

                    // Update cart with values from request body if available
                    if (requestMap.containsKey("address") || requestMap.containsKey("timeSlot")) {
                        String address = requestMap.containsKey("address") ?
                                (String) requestMap.get("address") : null;
                        String timeSlot = requestMap.containsKey("timeSlot") ?
                                (String) requestMap.get("timeSlot") : null;

                        LOG.info("Updating cart from request body - address: {}, timeSlot: {}",
                                address, timeSlot);

                        cartService.updateCartDetails(customerEmail, address, timeSlot);
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to parse request body: {}", e.getMessage());
                    // Continue with submission even if parsing fails
                }
            }

            // Submit the order
            Order order = cartService.submitOrder(customerEmail);
            if (order == null) {
                LOG.warn("Failed to submit order - order is null");
                return ApiResponse.error("Failed to submit order. Cart may be empty.");
            }

            LOG.info("Order submitted successfully with ID: {}, address: {}, timeSlot: {}",
                    order.getId(), order.getAddress(), order.getTimeSlot());

            // Format the response according to the expected structure
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("content", Collections.singletonList(order));
            return ApiResponse.success(responseBody);
        } catch (Exception e) {
            LOG.error("Error submitting order", e);
            return ApiResponse.serverError("Internal server error: " + e.getMessage());
        }
    }
}