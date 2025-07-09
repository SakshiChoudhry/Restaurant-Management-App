package com.restaurantapp.Repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class ReservationDeletionRepository {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationDeletionRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String bookingTableName = System.getenv("booking_table");

    @Inject
    public ReservationDeletionRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    /**
     * Get a reservation by ID
     * @param reservationId The reservation ID
     * @return The reservation details as a Map, or null if not found
     */
    public Map<String, AttributeValue> getReservationById(String reservationId) {
        try {
            LOG.info("Getting reservation with ID: {}", reservationId);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", AttributeValue.builder().s(reservationId).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(bookingTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item().isEmpty()) {
                LOG.warn("Reservation not found with ID: {}", reservationId);
                return null;
            }

            LOG.info("Found reservation with ID: {}", reservationId);
            return response.item();
        } catch (Exception e) {
            LOG.error("Error getting reservation by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving reservation", e);
        }
    }

    /**
     * Update the status of a reservation to Cancelled
     * @param reservationId The reservation ID
     * @return true if the update was successful, false otherwise
     */
    public boolean cancelReservation(String reservationId,String context) {
        try {
            LOG.info("Cancelling reservation with ID: {}", reservationId);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", AttributeValue.builder().s(reservationId).build());

            LOG.info("pUT IN THE MAP");

            Map<String, AttributeValueUpdate> updates = new HashMap<>();
            updates.put("status", AttributeValueUpdate.builder()
                    .value(AttributeValue.builder().s(context).build())
                    .action(AttributeAction.PUT)
                    .build());

            LOG.info("STATUS PUT IN MAP {}", updates);



            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(bookingTableName)
                    .key(key)
                    .attributeUpdates(updates)
                    .build();

            dynamoDbClient.updateItem(request);

            LOG.info("Successfully cancelled reservation: {}", reservationId);
            return true;
        } catch (Exception e) {
            LOG.error("Error cancelling reservation: {}", e.getMessage(), e);
            throw new RuntimeException("Error cancelling reservation", e);
        }
    }
}