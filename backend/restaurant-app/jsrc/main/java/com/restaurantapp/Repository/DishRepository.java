package com.restaurantapp.Repository;

import com.restaurantapp.Model.Dish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class DishRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DishRepository.class);
    private final String dishesTableName;
    private final DynamoDbClient dynamoDbClient;

    @Inject
    public DishRepository() {
        this.dishesTableName = System.getenv("dishes_table");
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
        LOG.info("DishRepository initialized with table: {}", dishesTableName);
    }

    public List<Dish> getAllDishes() {
        try {
            LOG.info("Fetching all dishes from table: {}", dishesTableName);

            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(dishesTableName)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<Dish> dishes = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                dishes.add(mapToDish(item));
            }

            LOG.info("Successfully retrieved {} dishes", dishes.size());
            return dishes;
        } catch (Exception e) {
            LOG.error("Error retrieving dishes from DynamoDB", e);
            return Collections.emptyList();
        }
    }

    public List<Dish> getDishesByType(String dishType) {
        try {
            // Handle null or empty dish type
            if (dishType == null || dishType.trim().isEmpty()) {
                LOG.warn("Empty dish type provided, returning all dishes");
                return getAllDishes();
            }

            // Normalize the dish type by trimming spaces
            String normalizedDishType = dishType.trim();

            LOG.info("Fetching dishes by type: '{}' from table: {}", normalizedDishType, dishesTableName);

            // Get all dishes and filter in memory for case-insensitive comparison
            List<Dish> allDishes = getAllDishes();
            List<Dish> filteredDishes = new ArrayList<>();

            for (Dish dish : allDishes) {
                if (dish.getDishType() != null &&
                        dish.getDishType().trim().equalsIgnoreCase(normalizedDishType)) {
                    filteredDishes.add(dish);
                }
            }

            LOG.info("Successfully filtered {} dishes of type: '{}' (case-insensitive)",
                    filteredDishes.size(), normalizedDishType);
            return filteredDishes;
        } catch (Exception e) {
            LOG.error("Error retrieving dishes by type from DynamoDB", e);
            return Collections.emptyList();
        }
    }

    public Dish getDishById(String id) {
        try {
            LOG.info("Fetching dish by ID: {} from table: {}", id, dishesTableName);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("dishId", AttributeValue.builder().s(id).build());

            GetItemRequest getItemRequest = GetItemRequest.builder()
                    .tableName(dishesTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(getItemRequest);

            if (!response.hasItem()) {
                LOG.info("No dish found with ID: {}", id);
                return null;
            }

            Dish dish = mapToDish(response.item());
            LOG.info("Successfully retrieved dish with ID: {}", id);
            return dish;
        } catch (Exception e) {
            LOG.error("Error retrieving dish by ID from DynamoDB", e);
            return null;
        }
    }

    private Dish mapToDish(Map<String, AttributeValue> item) {
        Dish dish = new Dish();

        if (item.containsKey("dishId")) {
            dish.setDishId(item.get("dishId").s());
        }
        if (item.containsKey("dishName")) {
            dish.setDishName(item.get("dishName").s());
        }
        if (item.containsKey("dishDescription")) {
            dish.setDishDescription(item.get("dishDescription").s());
        }
        if (item.containsKey("calories")) {
            dish.setCalories(item.get("calories").s());
        }
        if (item.containsKey("carbohydrates")) {
            dish.setCarbohydrates(item.get("carbohydrates").s());
        }
        if (item.containsKey("dishFrequency") && item.get("dishFrequency").n() != null) {
            try {
                dish.setDishFrequency(Integer.parseInt(item.get("dishFrequency").n()));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid dishFrequency value: {}", item.get("dishFrequency").n());
                dish.setDishFrequency(0);
            }
        }
        if (item.containsKey("imageUrl")) {
            dish.setImageUrl(item.get("imageUrl").s());
        }
        if (item.containsKey("price")) {
            if (item.get("price").n() != null) {
                try {
                    dish.setPrice(item.get("price").n());
                } catch (Exception e) {
                    LOG.warn("Invalid numeric price value: {}", item.get("price").n());
                    dish.setPrice("0.0");
                }
            } else if (item.get("price").s() != null) {
                dish.setPrice(item.get("price").s());
            } else {
                dish.setPrice("0.0");
            }
        } else {
            dish.setPrice("0.0");
        }
        if (item.containsKey("dishType")) {
            dish.setDishType(item.get("dishType").s());
        }
        if (item.containsKey("fats")) {
            dish.setFats(item.get("fats").s());
        }
        if (item.containsKey("proteins")) {
            dish.setProteins(item.get("proteins").s());
        }
        if (item.containsKey("state")) {
            dish.setState(item.get("state").bool());
        }
        if (item.containsKey("vitamins")) {
            dish.setVitamins(item.get("vitamins").s());
        }
        if (item.containsKey("weight")) {
            dish.setWeight(item.get("weight").s());
        }
        if (item.containsKey("locationId")) {
            dish.setLocationId(item.get("locationId").s());
        }

        return dish;
    }
}