package com.restaurantapp.Repository;

import com.restaurantapp.Model.Dish;
import com.restaurantapp.Model.PopularDishesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DishesRepository {
    private static final Logger LOG = LoggerFactory.getLogger(DishesRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String dishesTableName = System.getenv("dishes_table");
    private final String bookingTableName = System.getenv("booking_table");

    public DishesRepository() {
        // Create DynamoDB client
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1) // Change to your region
                .build();
    }

    /**
     * Retrieves popular dishes from DynamoDB
     * @return List of popular dishes
     */
    public List<PopularDishesResponse> getPopularDishes() {
        try {
            LOG.info("Retrieving popular dishes from DynamoDB");

            // Create a scan request to get popular dishes
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(dishesTableName)
                    .filterExpression("isPopular = :popular")
                    .expressionAttributeValues(
                            Map.of(":popular", AttributeValue.builder().s("true").build())
                    )
                    .limit(4) // Limit to 4 dishes
                    .build();

            // Execute the scan
            ScanResponse response = dynamoDbClient.scan(scanRequest);

            // Convert the DynamoDB items to PopularDishesResponse objects
            List<PopularDishesResponse> popularDishes = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                String name = item.get("dishName").s();
                String price = item.get("price").s();
                String weight = item.get("weight").s();
                String imageUrl = item.get("imageUrl").s();
                String isPopular = item.get("isPopular").s();

                popularDishes.add(new PopularDishesResponse(name, price, weight, imageUrl));
            }

            LOG.info("Retrieved {} popular dishes from DynamoDB", popularDishes.size());

            return popularDishes;

        } catch (Exception e) {
            LOG.error("Error retrieving popular dishes from DynamoDB", e);
            throw new RuntimeException("Failed to retrieve popular dishes from DynamoDB", e);
        }
    }
    public String getLocationIdFromReservation(String reservationId) {
        LOG.info("Fetching location ID for reservation: {}", reservationId);

        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(bookingTableName)
                .key(Map.of("reservationId", AttributeValue.builder().s(reservationId).build()))
                .build();

        try {
            Map<String, AttributeValue> item = dynamoDbClient.getItem(getRequest).item();

            if (item == null || item.isEmpty()) {
                LOG.warn("No reservation found with ID: {}", reservationId);
                return null;
            }

            String locationId = item.containsKey("locationId") ? item.get("locationId").s() : null;
            LOG.info("Found location ID: {} for reservation: {}", locationId, reservationId);
            return locationId;
        } catch (Exception e) {
            LOG.error("Error fetching location ID for reservation: {}", reservationId, e);
            return null;
        }
    }

    public List<Dish> getAvailableDishesByLocation(String locationId) {
        LOG.info("Fetching available dishes for location: {}", locationId);

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(dishesTableName)
                .filterExpression("locationId = :locId")
                .expressionAttributeValues(Map.of(
                        ":locId", AttributeValue.builder().s(locationId).build()
                ))
                .build();

        try {
            ScanResponse response = dynamoDbClient.scan(scanRequest);

            List<Dish> dishes = response.items().stream()
                    .map(this::mapItemToDish)
                    .collect(Collectors.toList());

            LOG.info("Found {} available dishes for location: {}", dishes.size(), locationId);
            return dishes;
        } catch (Exception e) {
            LOG.error("Error fetching available dishes for location: {}", locationId, e);
            return Collections.emptyList();
        }
    }

    public Dish getDishById(String dishId) {
        LOG.info("Getting dish by ID: {}", dishId);

        GetItemRequest getRequest = GetItemRequest.builder()
                .tableName(dishesTableName)
                .key(Map.of("dishId", AttributeValue.builder().s(dishId).build()))
                .build();

        try {
            Map<String, AttributeValue> item = dynamoDbClient.getItem(getRequest).item();

            if (item == null || item.isEmpty()) {
                LOG.warn("No dish found with ID: {}", dishId);
                return null;
            }

            Dish dish = new Dish();
            dish.setDishId(dishId);
            dish.setDishName(item.containsKey("dishName") ? item.get("dishName").s() : "");
            dish.setImageUrl(item.containsKey("imageUrl") ? item.get("imageUrl").s() : "");

            // Handle price as double
            if (item.containsKey("price")) {
                try {
                    dish.setPrice(item.get("price").s());
                } catch (Exception e) {
                    LOG.warn("Error parsing price for dish {}: {}", dishId, e.getMessage());
                    dish.setPrice("0.0");
                }
            }

            dish.setState(item.containsKey("state") ? item.get("state").bool(): false) ;
            dish.setLocationId(item.containsKey("locationId") ? item.get("locationId").s() : "");
            dish.setWeight(item.containsKey("weight") ? item.get("weight").s() : "");
            dish.setDishDescription(item.containsKey("dishDescription") ? item.get("dishDescription").s() : "");
            dish.setCalories(item.containsKey("calories") ? item.get("calories").s() : "");
            dish.setCarbohydrates(item.containsKey("carbohydrates") ? item.get("carbohydrates").s() : "");
            dish.setProteins(item.containsKey("proteins") ? item.get("proteins").s() : "");
            dish.setFats(item.containsKey("fats") ? item.get("fats").s() : "");
            dish.setVitamins(item.containsKey("vitamins") ? item.get("vitamins").s() : "");
            dish.setDishType(item.containsKey("dishType") ? item.get("dishType").s() : "");

            // Handle dishFrequency as int
            if (item.containsKey("dishFrequency")) {
                try {
                    dish.setDishFrequency(Integer.parseInt(item.get("dishFrequency").n()));
                } catch (Exception e) {
                    LOG.warn("Error parsing dishFrequency for dish {}: {}", dishId, e.getMessage());
                    dish.setDishFrequency(0);
                }
            }

            LOG.info("Found dish: {}, location: {}", dish.getDishName(), dish.getLocationId());
            return dish;
        } catch (Exception e) {
            LOG.error("Error getting dish by ID: {}", dishId, e);
            return null;
        }
    }

    private Dish mapItemToDish(Map<String, AttributeValue> item) {
        Dish dish = new Dish();

        dish.setDishId(item.containsKey("dishId") ? item.get("dishId").s() : "");
        dish.setDishName(item.containsKey("dishName") ? item.get("dishName").s() : "");
        dish.setImageUrl(item.containsKey("imageUrl") ? item.get("imageUrl").s() : "");
        dish.setState(item.containsKey("state") ? item.get("state").bool(): false);
        dish.setLocationId(item.containsKey("locationId") ? item.get("locationId").s() : "");

        // Parse price as double
        if (item.containsKey("price")) {
            try {
                dish.setPrice(item.get("price").s());
            } catch (NumberFormatException e) {
                LOG.error("Error parsing price for dish {}", dish.getDishId(), e);
                dish.setPrice("0.0");
            }
        } else {
            dish.setPrice("0.0");
        }
        dish.setWeight(item.containsKey("weight") ? item.get("weight").s() : "");
        dish.setDishDescription(item.containsKey("dishDescription") ? item.get("dishDescription").s() : "");
        dish.setCalories(item.containsKey("calories") ? item.get("calories").s() : "");
        dish.setCarbohydrates(item.containsKey("carbohydrates") ? item.get("carbohydrates").s() : "");
        dish.setProteins(item.containsKey("proteins") ? item.get("proteins").s() : "");
        dish.setFats(item.containsKey("fats") ? item.get("fats").s() : "");
        dish.setVitamins(item.containsKey("vitamins") ? item.get("vitamins").s() : "");
        dish.setDishType(item.containsKey("dishType") ? item.get("dishType").s() : "");

        // Parse dish frequency
        if (item.containsKey("dishFrequency")) {
            try {
                dish.setDishFrequency(Integer.parseInt(item.get("dishFrequency").n()));
            } catch (NumberFormatException e) {
                LOG.error("Error parsing dishFrequency for dish {}", dish.getDishId(), e);
                dish.setDishFrequency(0);
            }
        } else {
            dish.setDishFrequency(0);
        }
        return dish;
    }

}