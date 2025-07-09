package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Model.AvailableDishResponse;
import com.restaurantapp.Model.PopularDishesResponse;
import com.restaurantapp.Service.DishesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Controller for user management operations.
 */
@Singleton
public class DishesController {
    private static final Logger LOG = LoggerFactory.getLogger(DishesController.class);
    private final DishesService dishesService;


    private final ObjectMapper objectMapper;

    @Inject
    public DishesController(DishesService dishesService, ObjectMapper objectMapper) {
        this.dishesService = dishesService;
        this.objectMapper = objectMapper;
    }


    public APIGatewayProxyResponseEvent getPopularDishes() {
        try {
            LOG.info("Processing popular dishes request");

            // Get popular dishes from service
            List<PopularDishesResponse> popularDishes = dishesService.getPopularDishes();

            // Create success response with dishes
            return ApiResponse.success(Map.of(
                    "message", "Popular dishes retrieved successfully",
                    "dishes", popularDishes
            ));
        } catch (Exception e) {
            LOG.error("Error retrieving popular dishes", e);
            return ApiResponse.serverError("Error retrieving popular dishes: " + e.getMessage());
        }
    }
    public APIGatewayProxyResponseEvent getAvailableDishesForReservation(String reservationId) {
        try {
            LOG.info("Processing request for available dishes for reservation: {}", reservationId);
            List<AvailableDishResponse> dishes = dishesService.getAvailableDishesForReservation(reservationId);
            return ApiResponse.success(Map.of("content", dishes));
        } catch (IllegalArgumentException e) {
            LOG.warn("Bad request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error getting available dishes for reservation: {}", reservationId, e);
            return ApiResponse.serverError("Error retrieving available dishes: " + e.getMessage());
        }
    }

}
