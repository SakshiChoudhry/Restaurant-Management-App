package com.restaurantapp.Repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.restaurantapp.Model.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RestaurantLocationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RestaurantLocationRepository.class);

    private final DynamoDB dynamoDB;
    private final String tableName;

    public RestaurantLocationRepository() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
        this.tableName = System.getenv("location_table");
        LOG.info("Initialized RestaurantLocationRepository with table name: {}", tableName);
    }

    public List<Location> findAll() {
        LOG.info("Finding all locations from DynamoDB table: {}", tableName);
        List<Location> locations = new ArrayList<>();

        try {
            com.amazonaws.services.dynamodbv2.document.Table ddbTable = dynamoDB.getTable(tableName);
            ScanSpec scanSpec = new ScanSpec();

            ItemCollection<ScanOutcome> items = ddbTable.scan(scanSpec);
            Iterator<Item> iterator = items.iterator();

            while (iterator.hasNext()) {
                Item item = iterator.next();
                LOG.debug("Raw location data: {}", item.toJSON());
                locations.add(mapToLocation(item));
            }

            LOG.info("Found {} locations in DynamoDB", locations.size());
        } catch (Exception e) {
            LOG.error("Error fetching locations from DynamoDB", e);
            throw new RuntimeException("Error fetching locations from DynamoDB", e);
        }

        return locations;
    }

    private Location mapToLocation(Item item) {
        Location location = new Location();
        location.setLocationId(item.getString("locationId"));

        if (item.hasAttribute("address")) {
            location.setLocationAddress(item.getString("address"));
        }

        // Handle averageOccupancy as a string that might contain a percentage sign
        if (item.hasAttribute("averageOccupancy")) {
            try {
                String occupancyStr = item.getString("averageOccupancy");
                // Remove percentage sign if present and convert to integer
                occupancyStr = occupancyStr.replace("%", "").trim();
                location.setAverageOccupancy(occupancyStr);
            } catch (Exception e) {
                LOG.warn("Error parsing averageOccupancy for location {}: {}", item.getString("id"), e.getMessage());
                // Set a default value
                location.setAverageOccupancy("0");
            }
        }

        if (item.hasAttribute("description")) {
            location.setDescription(item.getString("description"));
        }

        if (item.hasAttribute("imageUrl")) {
            location.setImageURL(item.getString("imageUrl"));
        }

        // Handle rating as a string that might need conversion
        if (item.hasAttribute("rating")) {
            try {
                String ratingStr = item.getString("rating");
                location.setRating(ratingStr);
            } catch (Exception e) {
                LOG.warn("Error parsing rating for location {}: {}", item.getString("id"), e.getMessage());
                // Set a default value
                location.setRating("0.0");
            }
        }

        // Handle totalCapacity as a string that might need conversion
        if (item.hasAttribute("totalCapacity")) {
            try {
                String capacityStr = item.getString("totalCapacity");
                location.setTotalCapacity(capacityStr);
            } catch (Exception e) {
                LOG.warn("Error parsing totalCapacity for location {}: {}", item.getString("id"), e.getMessage());
                // Set a default value
                location.setTotalCapacity("0");
            }
        }

        return location;
    }
}