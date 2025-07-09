package com.restaurantapp.Service;

import com.restaurantapp.Model.*;
import com.restaurantapp.Repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Comparator;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class DishService {
    private static final Logger LOG = LoggerFactory.getLogger(DishService.class);
    private final DishRepository dishRepository;

    @Inject
    public DishService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
        LOG.info("DishService initialized");
    }

    /**
     * Get all dishes with simplified information for list view
     * @param queryParams Query parameters for filtering and sorting
     * @return List of simplified dish responses
     */
    public List<DishResponse> getAllDishes(Map<String, String> queryParams) {
        LOG.info("Getting simplified dishes with query params: {}", queryParams);

        // Get filtered and sorted dishes
        List<Dish> dishes = getFilteredAndSortedDishes(queryParams);

        // Convert to simplified DishResponse objects
        return dishes.stream()
                .map(DishResponse::fromDish)
                .collect(Collectors.toList());
    }

    /**
     * Get dish by ID with detailed information
     * @param id Dish ID
     * @return Detailed dish response
     */
    public DishResponseById getDishById(String id) {
        LOG.info("Getting dish by ID: {}", id);
        // Trim the ID to handle any extra spaces
        String trimmedId = id != null ? id.trim() : null;

        if (trimmedId == null || trimmedId.isEmpty()) {
            LOG.warn("Empty dish ID provided");
            return null;
        }

        Dish dish = dishRepository.getDishById(trimmedId);
        return dish != null ? DishResponseById.fromDish(dish) : null;
    }

    /**
     * Helper method to get filtered and sorted dishes with edge case handling
     */
    private List<Dish> getFilteredAndSortedDishes(Map<String, String> queryParams) {
        // Get all dishes or filter by type
        List<Dish> dishes;

        try {
            // Handle null queryParams
            if (queryParams == null || queryParams.isEmpty()) {
                LOG.info("No query parameters provided, returning all dishes");
                dishes = dishRepository.getAllDishes();
                return dishes;
            }

            // Filter by dish type if provided (use lowercase key for case-insensitivity)
            String dishType = null;
            // Look for dishType parameter with any case
            for (String key : queryParams.keySet()) {
                if (key.equalsIgnoreCase("dishtype")) {
                    dishType = queryParams.get(key);
                    break;
                }
            }

            if (dishType != null && !dishType.trim().isEmpty()) {
                LOG.info("Filtering dishes by type: '{}'", dishType);
                dishes = dishRepository.getDishesByType(dishType);

                // Check if any dishes were found
                if (dishes.isEmpty()) {
                    LOG.warn("No dishes found with type: '{}'", dishType);
                } else {
                    LOG.info("Found {} dishes with type: '{}'", dishes.size(), dishType);
                }
            } else {
                LOG.info("No dish type filter specified, retrieving all dishes");
                dishes = dishRepository.getAllDishes();

                // Check if any dishes were found
                if (dishes.isEmpty()) {
                    LOG.warn("No dishes found in the database");
                } else {
                    LOG.info("Retrieved {} dishes", dishes.size());
                }
            }

            // Apply sorting if specified (use lowercase key for case-insensitivity)
            String sort = null;
            // Look for sort parameter with any case
            for (String key : queryParams.keySet()) {
                if (key.equalsIgnoreCase("sort")) {
                    sort = queryParams.get(key);
                    break;
                }
            }

            if (sort != null && !sort.trim().isEmpty()) {
                dishes = sortDishes(dishes, sort.trim());
            }

            return dishes;
        } catch (Exception e) {
            LOG.error("Error while filtering and sorting dishes: {}", e.getMessage(), e);
            // Return empty list on error
            return Collections.emptyList();
        }
    }

    /**
     * Sort dishes based on sort parameter with edge case handling
     */
    /**
     * Sort dishes based on sort parameter with edge case handling
     */
    private List<Dish> sortDishes(List<Dish> dishes, String sortParam) {
        if (dishes == null || dishes.isEmpty()) {
            LOG.warn("Cannot sort empty or null dish list");
            return dishes;
        }

        if (sortParam == null || sortParam.isEmpty()) {
            LOG.warn("Sort parameter is null or empty, returning unsorted dishes");
            return dishes;
        }

        LOG.info("Sorting dishes by: '{}'", sortParam);

        try {
            // Normalize sort parameter by trimming and converting to lowercase
            String normalizedSortParam = sortParam.trim().toLowerCase();

            // Handle different sorting options
            if (normalizedSortParam.equals("popularity,asc") || normalizedSortParam.equals("popularity ascending")) {
                LOG.info("Sorting by popularity ascending");
                return dishes.stream()
                        .sorted(Comparator.comparingInt(dish -> {
                            // Handle null dish frequency
                            try {
                                return dish.getDishFrequency();
                            } catch (Exception e) {
                                LOG.warn("Error getting dish frequency, using default value 0");
                                return 0;
                            }
                        }))
                        .collect(Collectors.toList());
            } else if (normalizedSortParam.equals("popularity,desc") || normalizedSortParam.equals("popularity descending")) {
                LOG.info("Sorting by popularity descending");
                return dishes.stream()
                        .sorted(Comparator.comparingInt(dish -> {
                            // Handle null dish frequency
                            try {
                                return -dish.getDishFrequency(); // Negative for descending
                            } catch (Exception e) {
                                LOG.warn("Error getting dish frequency, using default value 0");
                                return 0;
                            }
                        }))
                        .collect(Collectors.toList());
            } else if (normalizedSortParam.equals("price,asc") || normalizedSortParam.equals("price ascending")) {
                LOG.info("Sorting by price ascending");
                return dishes.stream()
                        .sorted(Comparator.comparingDouble(dish -> {
                            // Handle null price
                            try {
                                // Parse the price string to a double
                                String priceStr = dish.getPrice().toString();
                                // Remove any currency symbols and parse
                                priceStr = priceStr.replaceAll("[^\\d.]", "");
                                return Double.parseDouble(priceStr);
                            } catch (Exception e) {
                                LOG.warn("Error parsing dish price in ascending order, using default value 0.0: {}", e.getMessage());
                                return 0.0;
                            }
                        }))
                        .collect(Collectors.toList());
            } else if (normalizedSortParam.equals("price,desc") || normalizedSortParam.equals("price descending")) {
                LOG.info("Sorting by price descending");
                return dishes.stream()
                        .sorted((dish1, dish2) -> {
                            try {
                                // Parse both prices and compare
                                String price1Str = dish1.getPrice().toString();
                                String price2Str = dish2.getPrice().toString();

                                // Remove any currency symbols and parse
                                price1Str = price1Str.replaceAll("[^\\d.]", "");
                                price2Str = price2Str.replaceAll("[^\\d.]", "");

                                double price1 = Double.parseDouble(price1Str);
                                double price2 = Double.parseDouble(price2Str);

                                // Compare in reverse order for descending
                                return Double.compare(price2, price1);
                            } catch (Exception e) {
                                LOG.warn("Error comparing dish prices in descending order: {}", e.getMessage());
                                return 0;
                            }
                        })
                        .collect(Collectors.toList());
            } else if (normalizedSortParam.equals("calories,asc") || normalizedSortParam.equals("calories ascending") ||
                    normalizedSortParam.equals("low calorie")) {
                LOG.info("Sorting by calories ascending (low calorie first)");
                return dishes.stream()
                        .sorted(Comparator.comparingInt(dish -> {
                            // Extract numeric calorie value
                            try {
                                String caloriesStr = dish.getCalories();
                                if (caloriesStr == null || caloriesStr.isEmpty()) {
                                    return Integer.MAX_VALUE; // Put dishes with no calories at the end
                                }

                                // Extract numeric value from string like "503 kcal"
                                String numericPart = caloriesStr.replaceAll("[^0-9]", "");
                                if (numericPart.isEmpty()) {
                                    return Integer.MAX_VALUE;
                                }

                                return Integer.parseInt(numericPart);
                            } catch (Exception e) {
                                LOG.warn("Error parsing calories value, using default MAX_VALUE");
                                return Integer.MAX_VALUE;
                            }
                        }))
                        .collect(Collectors.toList());
            } else if (normalizedSortParam.equals("calories,desc") || normalizedSortParam.equals("calories descending") ||
                    normalizedSortParam.equals("high calorie")) {
                LOG.info("Sorting by calories descending (high calorie first)");
                return dishes.stream()
                        .sorted(Comparator.comparingInt(dish -> {
                            // Extract numeric calorie value
                            try {
                                String caloriesStr = dish.getCalories();
                                if (caloriesStr == null || caloriesStr.isEmpty()) {
                                    return Integer.MIN_VALUE; // Put dishes with no calories at the end
                                }

                                // Extract numeric value from string like "503 kcal"
                                String numericPart = caloriesStr.replaceAll("[^0-9]", "");
                                if (numericPart.isEmpty()) {
                                    return Integer.MIN_VALUE;
                                }

                                return -Integer.parseInt(numericPart); // Negative for descending
                            } catch (Exception e) {
                                LOG.warn("Error parsing calories value, using default MIN_VALUE");
                                return Integer.MIN_VALUE;
                            }
                        }))
                        .collect(Collectors.toList());
            } else {
                LOG.warn("Invalid sort parameter: '{}', returning unsorted dishes", sortParam);
                return dishes;
            }
        } catch (Exception e) {
            LOG.error("Error while sorting dishes: {}", e.getMessage(), e);
            return dishes;
        }
    }
}