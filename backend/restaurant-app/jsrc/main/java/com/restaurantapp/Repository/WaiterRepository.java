package com.restaurantapp.Repository;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class WaiterRepository {
    private static final Logger LOG = LoggerFactory.getLogger(WaiterRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String waiterTableName = System.getenv("waiter_table");
    private final String userTableName = System.getenv("user_table");

    @Inject
    public WaiterRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    /**
     * Get waiter name by email
     * @param email The waiter's email
     * @return The waiter's name or "Unknown Waiter" if not found
     */
    public String getWaiterName(String email) {
        try {
            LOG.info("Getting name for waiter: {}", email);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(userTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item().isEmpty() || !response.item().containsKey("firstName")) {
                LOG.warn("Waiter name not found for email: {}", email);
                return "Unknown Waiter";
            }

            String firstNamename = response.item().get("firstName").s();
            String lastName = response.item().get("lastName").s();
            String name  = firstNamename + " " + lastName;
            LOG.info("Found name for waiter {}: {}", email, name);
            return name;
        } catch (Exception e) {
            LOG.error("Error getting waiter name: {}", e.getMessage(), e);
            return "Unknown Waiter";
        }
    }

    /**
     * Get waiter image URL by email
     * @param email The waiter's email
     * @return The waiter's image URL or default image if not found
     */
    public String getWaiterImageUrl(String email) {
        try {
            LOG.info("Getting image URL for waiter: {}", email);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(waiterTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item().isEmpty() || !response.item().containsKey("imageUrl")) {
                LOG.warn("Waiter image URL not found for email: {}", email);
                return "https://s3-alpha-sig.figma.com/img/a852/9476/63ac9702acd3da9fa577cb0df4b4364d?Expires=1745798400&Key-Pair-Id=APKAQ4GOSFWCW27IBOMQ&Signature=Dhp19G~65Le6Q7KVSCZWBSstYLKrhG11TbscuuGG1eepNIUsJ5DqQHWJAhdflXrc0xiSYyC0zVcTvpkfRd-Ly-kqXeRjQVDg-xfn0vN0anQdyrc0vxI3esPm45bVtKj2JTT2plhmfL5KRLxsimDsfLLFPjTNbZGggRJ-q-WpWVI9vM4Doz-d7NZ~eaKFCK3mo6WI8m~wDLzjnP1HNj2p8EqvKI-SHwf0YF6y~ulAeZhMNV09-LDeJol74PbRfahOPxYhVTWQ5lCUATTIx67jHv0LXz8n2JAjgwSy9WTwuJdvHlCtSAwPn~-YpISxKBRx1RNUMVxVbDkd2I1LQQnREg__";
            }

            String imageUrl = response.item().get("imageUrl").s();
            LOG.info("Found image URL for waiter {}: {}", email, imageUrl);
            return imageUrl;
        } catch (Exception e) {
            LOG.error("Error getting waiter image URL: {}", e.getMessage(), e);
            return "https://s3-alpha-sig.figma.com/img/a852/9476/63ac9702acd3da9fa577cb0df4b4364d?Expires=1745798400&Key-Pair-Id=APKAQ4GOSFWCW27IBOMQ&Signature=Dhp19G~65Le6Q7KVSCZWBSstYLKrhG11TbscuuGG1eepNIUsJ5DqQHWJAhdflXrc0xiSYyC0zVcTvpkfRd-Ly-kqXeRjQVDg-xfn0vN0anQdyrc0vxI3esPm45bVtKj2JTT2plhmfL5KRLxsimDsfLLFPjTNbZGggRJ-q-WpWVI9vM4Doz-d7NZ~eaKFCK3mo6WI8m~wDLzjnP1HNj2p8EqvKI-SHwf0YF6y~ulAeZhMNV09-LDeJol74PbRfahOPxYhVTWQ5lCUATTIx67jHv0LXz8n2JAjgwSy9WTwuJdvHlCtSAwPn~-YpISxKBRx1RNUMVxVbDkd2I1LQQnREg__";
        }
    }

    /**
     * Get waiter rating by email
     * @param email The waiter's email
     * @return The waiter's rating or "N/A" if not found
     */
    public String getWaiterRating(String email) {
        try {
            LOG.info("Getting rating for waiter: {}", email);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(waiterTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item().isEmpty() || !response.item().containsKey("rating")) {
                LOG.warn("Waiter rating not found for email: {}", email);
                return "N/A";
            }

            // Try to get rating as a number first
            if (response.item().get("rating").n() != null) {
                String rating = response.item().get("rating").n();
                LOG.info("Found rating for waiter {}: {}", email, rating);
                return rating;
            }

            // Fall back to string if number is not available
            if (response.item().get("rating").s() != null) {
                String rating = response.item().get("rating").s();
                LOG.info("Found rating for waiter {}: {}", email, rating);
                return rating;
            }

            LOG.warn("Could not parse rating for waiter: {}", email);
            return "N/A";
        } catch (Exception e) {
            LOG.error("Error getting waiter rating: {}", e.getMessage(), e);
            return "N/A";
        }
    }
}