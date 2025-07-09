package com.restaurantapp.Service;

import com.restaurantapp.Model.Cart;
import com.restaurantapp.Model.Dish;
import com.restaurantapp.Model.Order;
import com.restaurantapp.Repository.CartRepository;
import com.restaurantapp.Repository.DishesRepository;
import com.restaurantapp.Repository.OrderRepository;
import com.restaurantapp.Repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class CartService {
    private static final Logger LOG = LoggerFactory.getLogger(CartService.class);
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final DishesRepository dishesRepository;
    private final ReservationRepository reservationRepository;

    @Inject
    public CartService(CartRepository cartRepository, OrderRepository orderRepository,
                       DishesRepository dishesRepository) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.dishesRepository = dishesRepository;
        this.reservationRepository = new ReservationRepository();
    }

    public boolean addDishToCart(String customerEmail, String reservationId, String dishId) {
        LOG.info("Adding dish {} to cart for customer {} with reservation {}", dishId, customerEmail, reservationId);

        // First, get the reservation details to determine the location
        Map<String, String> reservationDetails = reservationRepository.getReservationDetails(reservationId);
        if (reservationDetails == null) {
            LOG.warn("Reservation {} not found", reservationId);
            return false;
        }

        String locationId = reservationDetails.get("locationId");
        if (locationId == null || locationId.isBlank()) {
            LOG.warn("Reservation {} does not have a locationId", reservationId);
            return false;
        }

        LOG.info("Reservation {} is for location {}", reservationId, locationId);

        // Check if dish exists and is available
        Dish dish = dishesRepository.getDishById(dishId);
        if (dish == null) {
            LOG.warn("Dish {} not found", dishId);
            return false;
        }

        // Verify dish is available
        if (!dish.isState()) {
            LOG.warn("Dish {} is not available. Current state: {}", dishId, dish.isState() ? "Available" : "On Stop");
            return false;
        }


        // Verify dish is available at this location
        if (dish.getLocationId() == null || !dish.getLocationId().equals(locationId)) {
            LOG.warn("Dish {} is not available at location {}. Dish location: {}",
                    dishId, locationId, dish.getLocationId());
            return false;
        }

        // Check if customer already has a cart with a different location
        Cart existingCart = cartRepository.getCartByCustomerEmail(customerEmail);
        if (existingCart != null && existingCart.getLocationId() != null
                && !existingCart.getLocationId().equals(locationId)) {
            LOG.warn("Customer {} already has a cart for location {}, cannot add dish from location {}",
                    customerEmail, existingCart.getLocationId(), locationId);
            return false;
        }

        // Add dish to cart
        return cartRepository.addDishToCart(customerEmail, reservationId, dish);
    }

    public Cart getCart(String customerEmail) {
        LOG.info("Fetching cart for customer: {}", customerEmail);
        Cart cart = cartRepository.getCartByCustomerEmail(customerEmail);

        if (cart != null && cart.getReservationId() != null) {
            // Ensure we have the latest reservation details
            updateCartWithReservationDetails(cart);
        }

        // Add extra debug logging
        if (cart != null) {
            LOG.info("Cart found with ID: {}, address: {}, timeSlot: {}, location: {}",
                    cart.getId(), cart.getAddress(), cart.getTimeSlot(), cart.getLocationId());
            if (cart.getOrderItems() != null) {
                LOG.info("Cart contains {} items", cart.getOrderItems().size());
            } else {
                LOG.warn("Cart orderItems is null");
            }
        } else {
            LOG.warn("No cart found for customer: {}", customerEmail);
        }

        return cart;
    }

    public Order submitOrder(String customerEmail) {
        LOG.info("Submitting order for customer: {}", customerEmail);

        // Get customer's cart with debug logging
        Cart cart = cartRepository.getCartByCustomerEmail(customerEmail);
        if (cart == null) {
            LOG.warn("No cart found for customer: {}", customerEmail);
            return null;
        }

        LOG.info("Retrieved cart with ID: {}", cart.getId());

        if (cart.getOrderItems() == null) {
            LOG.warn("Cart orderItems is null for customer: {}", customerEmail);
            return null;
        }

        if (cart.getOrderItems().isEmpty()) {
            LOG.warn("Cart is empty for customer: {}", customerEmail);
            return null;
        }

        LOG.info("Cart contains {} items for location {}",
                cart.getOrderItems().size(), cart.getLocationId());

        // Ensure we have the latest reservation details
        updateCartWithReservationDetails(cart);

        LOG.info("Submitting order with address: {}, timeSlot: {}", cart.getAddress(), cart.getTimeSlot());

        // Create order from cart
        String orderId = orderRepository.createOrderFromCart(cart);
        if (orderId == null) {
            LOG.error("Failed to create order for customer: {}", customerEmail);
            return null;
        }

        LOG.info("Created order with ID: {}", orderId);

        // Clear the cart
        boolean cleared = cartRepository.clearCart(customerEmail);
        if (!cleared) {
            LOG.warn("Failed to clear cart for customer: {}", customerEmail);
        } else {
            LOG.info("Cart cleared successfully for customer: {}", customerEmail);
        }

        // Return the created order
        Order order = orderRepository.getOrderById(orderId);
        if (order == null) {
            LOG.error("Failed to retrieve created order with ID: {}", orderId);
        } else {
            LOG.info("Retrieved order with ID: {}, address: {}, timeSlot: {}",
                    order.getId(), order.getAddress(), order.getTimeSlot());
        }

        return order;
    }

    /**
     * Update cart with reservation details
     */
    private void updateCartWithReservationDetails(Cart cart) {
        if (cart == null || cart.getReservationId() == null) {
            return;
        }

        LOG.info("Updating cart with reservation details for reservationId: {}", cart.getReservationId());

        Map<String, String> reservationDetails =
                reservationRepository.getReservationDetails(cart.getReservationId());

        if (reservationDetails != null) {
            LOG.info("Got reservation details: address={}, timeSlot={}, date={}, locationId={}",
                    reservationDetails.get("address"),
                    reservationDetails.get("timeSlot"),
                    reservationDetails.get("date"),
                    reservationDetails.get("locationId"));

            // Update cart with reservation details
            if (reservationDetails.containsKey("address")) {
                cart.setAddress(reservationDetails.get("address"));
            }

            if (reservationDetails.containsKey("timeSlot")) {
                cart.setTimeSlot(reservationDetails.get("timeSlot"));
            }

            if (reservationDetails.containsKey("date")) {
                cart.setDate(reservationDetails.get("date"));
            }

            if (reservationDetails.containsKey("locationId")) {
                cart.setLocationId(reservationDetails.get("locationId"));
            }
        } else {
            LOG.warn("No reservation details found for reservationId: {}", cart.getReservationId());
        }
    }

    /**
     * Update cart details with values from request
     */
    public boolean updateCartDetails(String customerEmail, String address, String timeSlot) {
        LOG.info("Updating cart details for customer: {}", customerEmail);

        Cart cart = cartRepository.getCartByCustomerEmail(customerEmail);
        if (cart == null) {
            LOG.warn("No cart found for customer: {}", customerEmail);
            return false;
        }

        boolean updated = false;

        if (address != null && !address.isBlank()) {
            cart.setAddress(address);
            updated = true;
            LOG.info("Updated cart address to: {}", address);
        }

        if (timeSlot != null && !timeSlot.isBlank()) {
            cart.setTimeSlot(timeSlot);
            updated = true;
            LOG.info("Updated cart timeSlot to: {}", timeSlot);
        }

        if (updated) {
            return cartRepository.updateCart(cart);
        }

        return true;
    }
}