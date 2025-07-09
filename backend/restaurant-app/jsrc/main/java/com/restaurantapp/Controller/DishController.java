package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Model.DishResponse;
import com.restaurantapp.Model.DishResponseById;
import com.restaurantapp.Model.DishesResponse;
import com.restaurantapp.Service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class DishController {
    private static final Logger LOG = LoggerFactory.getLogger(DishController.class);
    private final DishService dishService;
    private final ObjectMapper objectMapper;

    private static final Set<String> VALID_PARAMS = Set.of("dishtype", "sort");
    private static final Set<String> VALID_SORT_VALUES = Set.of(
            "popularity,asc", "popularity,desc",
            "price,asc", "price,desc",
            "calories,asc", "calories,desc",
            "popularity ascending", "popularity descending",
            "price ascending", "price descending",
            "calories ascending", "calories descending",
            "low calorie", "high calorie"
    );

    @Inject
    public DishController(DishService dishService, ObjectMapper objectMapper) {
        this.dishService = dishService;
        this.objectMapper = objectMapper;
        LOG.info("DishController initialized");
    }

    public APIGatewayProxyResponseEvent getDishes(Map<String, String> queryParams) {
        try {
            LOG.info("Getting dishes with query params: {}", queryParams);

            // Skip validation if no parameters
            if (queryParams == null || queryParams.isEmpty()) {
                List<DishResponse> dishes = dishService.getAllDishes(null);
                DishesResponse response = new DishesResponse(dishes);
                return ApiResponse.success(response);
            }

            // Validate query parameters
            Map<String, String> validatedParams = new HashMap<>();

            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                String paramName = entry.getKey();
                String paramValue = entry.getValue();

                // Check for valid parameter name (case-insensitive)
                String normalizedParamName = paramName.trim().toLowerCase();
                if (!VALID_PARAMS.contains(normalizedParamName)) {
                    return ApiResponse.error("Invalid query parameter: " + paramName);
                }

                // Check for empty parameter values
                if (paramValue == null || paramValue.trim().isEmpty()) {
                    return ApiResponse.error("Empty value for parameter: " + paramName);
                }

                // Normalize parameter value
                String normalizedParamValue = paramValue.trim();

                // Validate sort parameter values (case-insensitive)
                if (normalizedParamName.equals("sort")) {
                    String normalizedSortValue = normalizedParamValue.toLowerCase();
                    if (!VALID_SORT_VALUES.contains(normalizedSortValue)) {
                        return ApiResponse.error("Invalid sort value: " + paramValue +
                                ". Valid values are: Popularity Ascending, Popularity Descending, " +
                                "Price Ascending, Price Descending, Calories Ascending, Calories Descending, " +
                                "Low Calorie, High Calorie");
                    }
                    // Store the normalized value
                    validatedParams.put(normalizedParamName, normalizedParamValue);
                } else {
                    // Store other parameters with their original values (but trimmed)
                    validatedParams.put(normalizedParamName, normalizedParamValue);
                }
            }

            // Use the validated parameters for the service call
            List<DishResponse> dishes = dishService.getAllDishes(validatedParams);
            DishesResponse response = new DishesResponse(dishes);

            return ApiResponse.success(response);
        } catch (Exception e) {
            LOG.error("Error getting dishes", e);
            return ApiResponse.serverError("Error retrieving dishes: " + e.getMessage());
        }
    }

    public APIGatewayProxyResponseEvent getDishById(String id) {
        try {
            LOG.info("Getting dish by ID: {}", id);
            // Trim the ID to handle any extra spaces
            String trimmedId = id != null ? id.trim() : null;

            if (trimmedId == null || trimmedId.isEmpty()) {
                return ApiResponse.error("Dish ID cannot be empty");
            }

            DishResponseById dish = dishService.getDishById(trimmedId);

            if (dish == null) {
                return ApiResponse.notFound("Dish not found with ID: " + trimmedId);
            }

            return ApiResponse.success(dish);
        } catch (Exception e) {
            LOG.error("Error getting dish by ID", e);
            return ApiResponse.serverError("Error retrieving dish: " + e.getMessage());
        }
    }

    public APIGatewayProxyResponseEvent getPopularDishes() {
        try {
            LOG.info("Getting popular dishes");
            // Get all dishes and sort by popularity (dishFrequency)
            Map<String, String> sortParams = new HashMap<>();
            sortParams.put("sort", "popularity,desc");

            List<DishResponse> allDishes = dishService.getAllDishes(sortParams);

            // Take top 5 or fewer if there aren't 5 dishes
            List<DishResponse> popularDishes = allDishes.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            DishesResponse response = new DishesResponse(popularDishes);
            return ApiResponse.success(response);
        } catch (Exception e) {
            LOG.error("Error getting popular dishes", e);
            return ApiResponse.serverError("Error retrieving popular dishes: " + e.getMessage());
        }
    }
}