package com.restaurantapp.Repository;

import com.restaurantapp.Model.User;
import com.restaurantapp.Model.Waiter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final DynamoDbClient dynamoDbClient;

    private final String tableName = System.getenv("user_table");
    private final String tableWaiterName = System.getenv("waiter_table");

    public UserRepository() {
        // Create DynamoDB client
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1) // Change to your region
                .build();
    }

    public boolean emailExists(String email) {
        try {
            // Create key condition
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            // Create request
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            // Execute request
            GetItemResponse response = dynamoDbClient.getItem(request);

            // If the item exists, response.item() will not be empty
            return !response.item().isEmpty();
        } catch (Exception e) {
            System.err.println("Error checking if email exists: " + e.getMessage());
            throw new RuntimeException("Error checking if email exists", e);
        }
    }


    // Waiter exists
    public boolean waiterExists(String email) {
        try {
            // Create key condition
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            // Create request
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableWaiterName)
                    .key(key)
                    .build();

            // Execute request
            GetItemResponse response = dynamoDbClient.getItem(request);

            // If the item exists, response.item() will not be empty
            return !response.item().isEmpty();
        } catch (Exception e) {
            System.err.println("Error checking if email exists IN WAITER: " + e.getMessage());
            throw new RuntimeException("Error checking if email exists IN WAITER", e);
        }
    }

    public void createUser(User user) {
        try {
            // Create item attributes
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("email", AttributeValue.builder().s(user.getEmail()).build());
            item.put("firstName", AttributeValue.builder().s(user.getFirstName()).build());
            item.put("lastName", AttributeValue.builder().s(user.getLastName()).build());
            item.put("failedAttempts", AttributeValue.builder().n("0").build());

            if (waiterExists(user.getEmail())) {
                item.put("role", AttributeValue.builder().s("WAITER").build());
            }else {
                item.put("role", AttributeValue.builder().s("CUSTOMER").build());
            }
            // Create request
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
            dynamoDbClient.putItem(request);

            // Execute request
            System.out.println("User created successfully: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Error creating user: " + e.getMessage());
            throw new RuntimeException("Error creating user", e);
        }
    }


    public void createWaiter(Waiter user) {
        try {
            // Create item attributes
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("email", AttributeValue.builder().s(user.getEmail()).build());


            if (user.getLocationId() != null && !user.getLocationId().isEmpty()) {
                item.put("locationId", AttributeValue.builder().s(user.getLocationId()).build());
            }

            // Create request
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableWaiterName)
                    .item(item)
                    .build();
            dynamoDbClient.putItem(request);

            // Execute request
            System.out.println("Waiter created successfully: " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Error creating waiter: " + e.getMessage());
            throw new RuntimeException("Error creating waiter", e);
        }
    }


    public User getUserByEmail(String email) {
        try {
            System.out.println("Getting user by email: " + email);

            // Create key condition
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            // Create request
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            // Execute request
            GetItemResponse response = dynamoDbClient.getItem(request);

            // If user not found
            if (response.item().isEmpty()) {
                System.out.println("User not found: " + email);
                return null;
            }

            // Map DynamoDB item to User object
            Map<String, AttributeValue> item = response.item();
            User user = new User();
            user.setEmail(item.get("email").s());
            user.setFirstName(item.get("firstName").s());
            user.setLastName(item.get("lastName").s());
            user.setRole(item.get("role").s());

            // Map account locking fields
            if (item.containsKey("failedAttempts")) {
                user.setFailedAttempts(Integer.parseInt(item.get("failedAttempts").n()));
            } else {
                user.setFailedAttempts(0);
            }

            if (item.containsKey("lockedUntil")) {
                user.setLockedUntil(Long.parseLong(item.get("lockedUntil").n()));
            }

            System.out.println("User found: " + user.getEmail());
            return user;
        } catch (Exception e) {
            System.err.println("Error getting user by email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error getting user by email", e);
        }
    }

    /**
     * Increments the failed login attempts for a user
     *
     * @param email The user's email
     * @param failedAttempts The new failed attempts count
     */
    public void incrementFailedAttempts(String email, int failedAttempts) {
        try {
            System.out.println("Incrementing failed attempts for user: " + email + " to " + failedAttempts);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("failedAttempts", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n(String.valueOf(failedAttempts)).build())
                    .action(AttributeAction.PUT)
                    .build());

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build();

            dynamoDbClient.updateItem(request);
            System.out.println("Updated failed attempts for user: " + email);
        } catch (Exception e) {
            System.err.println("Error updating failed attempts for user: " + e.getMessage());
            throw new RuntimeException("Error updating failed attempts", e);
        }
    }

    /**
     * Locks a user account after too many failed attempts
     *
     * @param email The user's email
     * @param failedAttempts The failed attempts count
     * @param lockedUntil Timestamp until which the account is locked
     */
    public void lockAccount(String email, int failedAttempts, long lockedUntil) {
        try {
            System.out.println("Locking account for user: " + email + " until " + new Date(lockedUntil));

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("failedAttempts", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n(String.valueOf(failedAttempts)).build())
                    .action(AttributeAction.PUT)
                    .build());
            updates.put("lockedUntil", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n(String.valueOf(lockedUntil)).build())
                    .action(AttributeAction.PUT)
                    .build());

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build();

            dynamoDbClient.updateItem(request);
            System.out.println("Locked account for user: " + email + " until " + new Date(lockedUntil));
        } catch (Exception e) {
            System.err.println("Error locking account for user: " + e.getMessage());
            throw new RuntimeException("Error locking account", e);
        }
    }

    /**
     * Resets failed attempts counter after successful login
     *
     * @param email The user's email
     */
    public void resetFailedAttempts(String email) {
        try {
            System.out.println("Resetting failed attempts for user: " + email);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("email", AttributeValue.builder().s(email).build());

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("failedAttempts", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().n("0").build())
                    .action(AttributeAction.PUT)
                    .build());

            // Remove the lockedUntil attribute if it exists
            updates.put("lockedUntil", AttributeValueUpdate.builder()
                    .action(AttributeAction.DELETE)
                    .build());

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build();

            dynamoDbClient.updateItem(request);
            System.out.println("Reset failed attempts for user after successful login: " + email);
        } catch (Exception e) {
            System.err.println("Error resetting failed attempts for user: " + e.getMessage());
            throw new RuntimeException("Error resetting failed attempts", e);
        }
    }
}