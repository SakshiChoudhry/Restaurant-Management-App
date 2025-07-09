package com.restaurantapp.Repository;

import com.restaurantapp.Model.Location;
import com.restaurantapp.Model.Reservation;
import com.restaurantapp.Model.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ReservationRepository
{
    private static final Logger LOG = LoggerFactory.getLogger(ReservationRepository.class);

    private final DynamoDbClient dynamoDbClient;
    private final String reservationTableName;
    private final String locationTableName;

    // Hardcoded slot map
    private final Map<String, Slot> slotMap;

    @Inject
    public ReservationRepository()
    {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();

        this.reservationTableName = System.getenv("booking_table");
        this.locationTableName = System.getenv("location_table");

        LOG.info("Initialized ReservationRepository with tables:");
        LOG.info("Reservation table: {}", reservationTableName);
        LOG.info("Location table: {}", locationTableName);

        // Initialize hardcoded slot map
        this.slotMap = initializeSlotMap();
        LOG.info("Initialized slot map with {} entries", slotMap.size());
    }

    /**
     * Initialize the hardcoded slot map with predefined values
     */
    private Map<String, Slot> initializeSlotMap()
    {
        Map<String, Slot> map = new HashMap<>();

        // Breakfast slots
        addSlot(map, "1", "10:30 a.m.", "12:00 p.m");
        addSlot(map, "2", "12:15 p.m.", "1:45 p.m");

        // Lunch slots
        addSlot(map, "3", "2:00 p.m.", "3:30 p.m");
        addSlot(map, "4", "3:45 p.m.", "5:15 p.m");

        // Dinner slots
        addSlot(map, "5", "5:30 p.m.", "7:00 p.m");
        addSlot(map, "6", "7:15 p.m.", "8:45 p.m");
        addSlot(map, "7", "9:00 p.m.", "10:30 p.m");

        return map;
    }

    /**
     * Helper method to add a slot to the map
     */
    private void addSlot(Map<String, Slot> map, String slotId, String startTime, String endTime)
    {
        Slot slot = new Slot();
        slot.setSlotId(slotId);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        map.put(slotId, slot);
    }

    public List<Reservation> findReservationsByEmail(String email)
    {
        try {
            LOG.info("Finding reservations for email: {}", email);
            LOG.debug("Using reservation table: {}", reservationTableName);

            // Check if table name is available
            if (reservationTableName == null || reservationTableName.isEmpty()) {
                LOG.error("Reservation table name is null or empty");
                throw new RuntimeException("Reservation table name is not configured");
            }

            List<Reservation> reservations = new ArrayList<>();

            // Try with CustomerEmail first
            try {
                Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                expressionAttributeValues.put(":email", AttributeValue.builder().s(email).build());

                ScanRequest scanRequest = ScanRequest.builder()
                        .tableName(reservationTableName)
                        .filterExpression("customerEmail = :email")
                        .expressionAttributeValues(expressionAttributeValues)
                        .build();

                ScanResponse response = dynamoDbClient.scan(scanRequest);

                for (Map<String, AttributeValue> item : response.items()) {
                    try {
                        Reservation reservation = mapToReservation(item);
                        reservations.add(reservation);
                    } catch (Exception e) {
                        LOG.error("Error mapping item to reservation: {}", e.getMessage());
                        LOG.error("Problematic item: {}", item);
                    }
                }

                LOG.info("Found {} reservations with CustomerEmail", reservations.size());
            }
            catch (Exception e)
            {
                LOG.warn("Error scanning with CustomerEmail: {}", e.getMessage());
            }

            // If no results with CustomerEmail, try with Email
            if (reservations.isEmpty()) {
                try {
                    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                    expressionAttributeValues.put(":email", AttributeValue.builder().s(email).build());

                    ScanRequest scanRequest = ScanRequest.builder()
                            .tableName(reservationTableName)
                            .filterExpression("email = :email")
                            .expressionAttributeValues(expressionAttributeValues)
                            .build();

                    ScanResponse response = dynamoDbClient.scan(scanRequest);

                    for (Map<String, AttributeValue> item : response.items()) {
                        try {
                            Reservation reservation = mapToReservation(item);
                            reservations.add(reservation);
                        } catch (Exception e) {
                            LOG.error("Error mapping item to reservation: {}", e.getMessage());
                            LOG.error("Problematic item: {}", item);
                        }
                    }

                    LOG.info("Found {} reservations with Email", reservations.size());
                } catch (Exception e) {
                    LOG.warn("Error scanning with Email: {}", e.getMessage());
                }
            }

            LOG.info("Found {} total reservations for email: {}", reservations.size(), email);
            return reservations;
        } catch (Exception e) {
            LOG.error("Error finding reservations by email: {}", e.getMessage(), e);
            throw new RuntimeException("Error finding reservations by email: " + e.getMessage(), e);
        }
    }

    public Location getLocationById(String locationId)
    {
        try {
            LOG.info("Getting location by ID: {}", locationId);

            if (locationTableName == null || locationTableName.isEmpty()) {
                LOG.error("Location table name is null or empty");
                return null;
            }

            // Create key condition
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("locationId", AttributeValue.builder().s(locationId).build());
            LOG.info("Nab=ndini");
            // Create request
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(locationTableName)
                    .key(key)
                    .build();

            LOG.info("Nandini");
            // Execute request
            GetItemResponse response = dynamoDbClient.getItem(request);
            LOG.info("sharda");
            // If location not found
            if (response.item().isEmpty()) {
                LOG.warn("Location not found: {}", locationId);
                return null;
            }

            // Map DynamoDB item to Location object
            Map<String, AttributeValue> item = response.item();
            Location location = new Location();
            location.setLocationId(item.get("locationId").s());

            // Handle potential missing address
            if (item.containsKey("address")) {
                LOG.info("cc", item.get("address").s());
                location.setLocationAddress(item.get("address").s());
            } else {
                location.setLocationAddress("Address not available");
            }

            LOG.info("Found location: {}", location.getLocationId());
            return location;
        } catch (ResourceNotFoundException e) {
            LOG.error("Location table '{}' does not exist", locationTableName, e);
            return null;
        } catch (Exception e) {
            LOG.error("Error getting location by ID: {}", locationId, e);
            return null; // Return null instead of throwing exception to make code more resilient
        }
    }

    /**
     * Get slot by ID from the hardcoded map
     */
    public Slot getSlotById(String slotId) {
        LOG.info("Getting slot by ID from hardcoded map: {}", slotId);

        if (slotId == null || slotId.isEmpty()) {
            LOG.warn("Slot ID is null or empty");
            return null;
        }

        Slot slot = slotMap.get(slotId);

        if (slot == null) {
            LOG.warn("Slot not found in hardcoded map: {}", slotId);
            return null;
        }

        LOG.info("Found slot in map: {} ({} - {})",
                slot.getSlotId(), slot.getStartTime(), slot.getEndTime());
        return slot;
    }

    private Reservation mapToReservation(Map<String, AttributeValue> item)
    {
        Reservation reservation = new Reservation();

        try {
            // Handle ReservationId
            if (item.containsKey("reservationId")) {
                reservation.setReservationId(item.get("reservationId").s());
            }

            // Handle TableId
            if (item.containsKey("tableId")) {
                reservation.setTableId(item.get("tableId").s());
            }

            // Handle CustomerEmail - try both possible attribute names
            if (item.containsKey("customerEmail"))
            {
                reservation.setCustomerEmail(item.get("customerEmail").s());
            }

            // Handle SlotId
            if (item.containsKey("slotId")) {
                reservation.setSlotId(item.get("slotId").s());
            }

            // Handle WaiterEmail
            if (item.containsKey("waiterEmail")) {
                reservation.setWaiterEmail(item.get("waiterEmail").s());
            }

            // Handle Status
            if (item.containsKey("status")) {
                reservation.setStatus(item.get("status").s());
            }

            // Handle LocationId
            if (item.containsKey("locationId")) {
                reservation.setLocationId(item.get("locationId").s());
            }

            // Handle Date - from reservation table
            if (item.containsKey("date")) {
                reservation.setDate(item.get("date").s());
            }

            // Handle NumberOfGuests
            if (item.containsKey("numberOfGuests"))
            {
                AttributeValue guestsValue = item.get("numberOfGuests");
                if (guestsValue.n() != null) {
                    reservation.setNumberOfGuests(Integer.parseInt(guestsValue.n()));
                } else if (guestsValue.s() != null) {
                    reservation.setNumberOfGuests(Integer.parseInt(guestsValue.s()));
                }
            } else if (item.containsKey("Guests")) {
                AttributeValue guestsValue = item.get("Guests");
                if (guestsValue.n() != null) {
                    reservation.setNumberOfGuests(Integer.parseInt(guestsValue.n()));
                } else if (guestsValue.s() != null) {
                    reservation.setNumberOfGuests(Integer.parseInt(guestsValue.s()));
                }
            }
        }
        catch (Exception e) {
            LOG.error("Error mapping item to reservation: {}", e.getMessage());
            LOG.error("Item content: {}", item);
            throw new RuntimeException("Error mapping item to reservation: " + e.getMessage(), e);
        }

        return reservation;
    }
    public Map<String, String> getReservationDetails(String reservationId) {
        LOG.info("Fetching reservation details for ID: {}", reservationId);

        try {
            // Create key condition
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", AttributeValue.builder().s(reservationId).build());

            // Create request
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(reservationTableName)
                    .key(key)
                    .build();

            // Execute request
            GetItemResponse response = dynamoDbClient.getItem(request);

            // If reservation not found
            if (response.item().isEmpty()) {
                LOG.warn("Reservation not found: {}", reservationId);
                return null;
            }

            Map<String, AttributeValue> item = response.item();
            Map<String, String> details = new HashMap<>();

            // Extract basic reservation details
            if (item.containsKey("customerEmail")) {
                details.put("customerEmail", item.get("customerEmail").s());
            }

            if (item.containsKey("date")) {
                details.put("date", item.get("date").s());
            }

            if (item.containsKey("slotId")) {
                String slotId = item.get("slotId").s();
                details.put("slotId", slotId);

                // Get time slot details from the slot map
                Slot slot = getSlotById(slotId);
                if (slot != null) {
                    details.put("timeSlot", slot.getStartTime() + " - " + slot.getEndTime());
                } else {
                    details.put("timeSlot", "");
                }
            }

            if (item.containsKey("locationId")) {
                String locationId = item.get("locationId").s();
                details.put("locationId", locationId);

                // Get location address
                Location location = getLocationById(locationId);
                if (location != null) {
                    details.put("address", location.getLocationAddress());
                } else {
                    details.put("address", "");
                }
            }

            LOG.info("Retrieved reservation details: date={}, timeSlot={}, address={}",
                    details.get("date"), details.get("timeSlot"), details.get("address"));

            return details;
        } catch (Exception e) {
            LOG.error("Error fetching reservation details for ID: {}", reservationId, e);
            return null;
        }
    }
    // Add getters for table names to help with debugging
    public String getReservationTableName() {
        return reservationTableName;
    }

    public String getLocationTableName()
    {
        return locationTableName;
    }
}
