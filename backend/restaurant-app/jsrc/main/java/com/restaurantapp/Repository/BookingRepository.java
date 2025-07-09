package com.restaurantapp.Repository;

import com.restaurantapp.Controller.ReservationWaiterController;
import com.restaurantapp.Model.Booking;
import com.restaurantapp.Model.TimeSlot;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class BookingRepository {
    private static final Logger LOG = LoggerFactory.getLogger(BookingRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String bookingTableName = System.getenv("booking_table");
    private final String waiterTableName = System.getenv("waiter_table");
    private final String locationTableName = System.getenv("location_table");
    private final String diningTableName = System.getenv("tables_table_name");

    // Maximum number of tables a waiter can handle
    private static final int MAX_TABLES_PER_WAITER = 4;

    @Inject
    public BookingRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    /**
     * Update an existing booking
     * @param booking The updated booking information
     * @return The updated booking
     */
    public Booking updateBooking(Booking booking) {
        try {
            LOG.info("Updating booking with ID: {}", booking.getReservationId());

            // Create item attributes
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("reservationId", AttributeValue.builder().s(booking.getReservationId()).build());
            item.put("tableId", AttributeValue.builder().s(booking.getTableId()).build());
            item.put("customerEmail", AttributeValue.builder().s(booking.getCustomerEmail()).build());
            item.put("slotId", AttributeValue.builder().s(booking.getSlotId()).build());
            item.put("date", AttributeValue.builder().s(booking.getDate()).build());
            item.put("waiterEmail", AttributeValue.builder().s(booking.getWaiterEmail()).build());
            item.put("status", AttributeValue.builder().s(booking.getStatus()).build());
            item.put("preOrderId", AttributeValue.builder().s(booking.getPreOrderId()).build());
            item.put("locationId", AttributeValue.builder().s(booking.getLocationId()).build());
            item.put("numberOfGuests", AttributeValue.builder().s(booking.getNumberOfGuests()).build());

            // Add secretCode if present
            if (booking.getSecretCode() != null && !booking.getSecretCode().isEmpty()) {
                item.put("secretCode", AttributeValue.builder().s(booking.getSecretCode()).build());
            }

            // Create request
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(bookingTableName)
                    .item(item)
                    .build();

            // Execute request
            dynamoDbClient.putItem(request);
            LOG.info("Booking updated successfully: {}", booking.getReservationId());

            return booking;
        } catch (Exception e) {
            LOG.error("Error updating booking: {}", e.getMessage());
            throw new RuntimeException("Error updating booking", e);
        }
    }

    /**
     * Check if a table is already booked for a specific date and time slot, excluding the current booking
     * @param locationId The location ID
     * @param tableId The table ID
     * @param date The date of booking
     * @param slotId The time slot ID
     * @param currentReservationId The current reservation ID to exclude
     * @return true if the table is already booked by someone else, false otherwise
     */
    public boolean isTableAlreadyBookedExcludingCurrent(String locationId, String tableId, String date, String slotId, String currentReservationId) {
        try {
            LOG.info("Checking if table {} at location {} is already booked for date {} and slot {}, excluding reservation {}",
                    tableId, locationId, date, slotId, currentReservationId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":tableId", AttributeValue.builder().s(tableId).build());
            expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());
            expressionAttributeValues.put(":slotId", AttributeValue.builder().s(slotId).build());
            expressionAttributeValues.put(":status", AttributeValue.builder().s("Reserved").build());
            expressionAttributeValues.put(":reservationId", AttributeValue.builder().s(currentReservationId).build());

            // Create expression attribute names for reserved keywords
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#date", "date");
            expressionAttributeNames.put("#status", "status");

            // Create scan request with filter and expression attribute names
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(bookingTableName)
                    .filterExpression("locationId = :locationId AND tableId = :tableId AND #date = :date AND slotId = :slotId AND #status = :status AND reservationId <> :reservationId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            boolean isBooked = !response.items().isEmpty();
            LOG.info("Table {} at location {} for date {} and slot {} is already booked by someone else: {}",
                    tableId, locationId, date, slotId, isBooked);

            return isBooked;
        } catch (Exception e) {
            LOG.error("Error checking if table is already booked: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking booking availability", e);
        }
    }

    public Booking createBooking(Booking booking) {
        try {
            // Create item attributes
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("reservationId", AttributeValue.builder().s(booking.getReservationId()).build());
            item.put("tableId", AttributeValue.builder().s(booking.getTableId()).build());
            item.put("customerEmail", AttributeValue.builder().s(booking.getCustomerEmail()).build());
            item.put("slotId", AttributeValue.builder().s(booking.getSlotId()).build());
            item.put("date", AttributeValue.builder().s(booking.getDate()).build());
            item.put("waiterEmail", AttributeValue.builder().s(booking.getWaiterEmail()).build());
            item.put("status", AttributeValue.builder().s(booking.getStatus()).build());
            item.put("preOrderId", AttributeValue.builder().s(booking.getPreOrderId()).build());
            item.put("locationId", AttributeValue.builder().s(booking.getLocationId()).build());
            item.put("numberOfGuests", AttributeValue.builder().s(booking.getNumberOfGuests()).build());

            // Add secretCode if present
            if (booking.getSecretCode() != null && !booking.getSecretCode().isEmpty()) {
                item.put("secretCode", AttributeValue.builder().s(booking.getSecretCode()).build());
            }

            // Create request
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(bookingTableName)
                    .item(item)
                    .build();

            // Execute request
            dynamoDbClient.putItem(request);
            LOG.info("Booking created successfully: {}", booking.getReservationId());

            return booking;
        } catch (Exception e) {
            LOG.error("Error creating booking: {}", e.getMessage());
            throw new RuntimeException("Error creating booking", e);
        }
    }

    /**
     * Check if a table exists at a specific location
     * @param locationId The location ID
     * @param tableNumber The table number
     * @return true if the table exists at the location, false otherwise
     */
    public boolean isTableValidForLocation(String locationId, String tableNumber) {
        try {
            LOG.info("Checking if table {} exists at location {}", tableNumber, locationId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":tableNumber", AttributeValue.builder().s(tableNumber).build());

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(diningTableName)
                    .filterExpression("locationId = :locationId AND tableNumber = :tableNumber")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            boolean isValid = !response.items().isEmpty();

            LOG.info("Table {} at location {} is valid: {}", tableNumber, locationId, isValid);
            return isValid;
        } catch (Exception e) {
            LOG.error("Error checking if table is valid for location: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking table validity", e);
        }
    }

    /**
     * Get the capacity of a table at a specific location
     * @param locationId The location ID
     * @param tableNumber The table number
     * @return The table capacity, or -1 if the table doesn't exist
     */
    public int getTableCapacity(String locationId, String tableNumber) {
        try {
            LOG.info("Getting capacity for table {} at location {}", tableNumber, locationId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":tableNumber", AttributeValue.builder().s(tableNumber).build());

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(diningTableName)
                    .filterExpression("locationId = :locationId AND tableNumber = :tableNumber")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            if (response.items().isEmpty()) {
                LOG.warn("Table {} not found at location {}", tableNumber, locationId);
                return -1;
            }

            // Get the capacity from the first (and should be only) item
            Map<String, AttributeValue> item = response.items().get(0);
            if (!item.containsKey("capacity")) {
                LOG.warn("Capacity not found for table {} at location {}", tableNumber, locationId);
                return -1;
            }

            AttributeValue capacityAttr = item.get("capacity");
            if (capacityAttr == null || capacityAttr.s() == null) {
                LOG.warn("Capacity value is null for table {} at location {}", tableNumber, locationId);
                return -1;
            }

            try {
                int capacity = Integer.parseInt(capacityAttr.s());
                LOG.info("Table {} at location {} has capacity {}", tableNumber, locationId, capacity);
                return capacity;
            } catch (NumberFormatException e) {
                LOG.warn("Invalid capacity format for table {} at location {}: {}",
                        tableNumber, locationId, capacityAttr.s());
                return -1;
            }
        } catch (Exception e) {
            LOG.error("Error getting table capacity: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting table capacity", e);
        }
    }

    /**
     * Check if a table has sufficient capacity for a number of guests
     * @param locationId The location ID
     * @param tableNumber The table number
     * @param numberOfGuests The number of guests
     * @return true if the table has sufficient capacity, false otherwise
     */
    public boolean hasTableSufficientCapacity(String locationId, String tableNumber, String numberOfGuests) {
        try {
            LOG.info("Checking if table {} at location {} has sufficient capacity for {} guests",
                    tableNumber, locationId, numberOfGuests);

            // First check if the table exists at the location
            if (!isTableValidForLocation(locationId, tableNumber)) {
                LOG.warn("Table {} not found at location {}", tableNumber, locationId);
                return false;
            }

            // Get the table capacity
            int capacity = getTableCapacity(locationId, tableNumber);
            if (capacity == -1) {
                LOG.warn("Could not determine capacity for table {} at location {}", tableNumber, locationId);
                return false;
            }

            // Check if the capacity is sufficient
            int guests = Integer.parseInt(numberOfGuests);
            boolean isSufficient = capacity >= guests;

            LOG.info("Table {} at location {} has capacity {} for {} guests. Sufficient: {}",
                    tableNumber, locationId, capacity, guests, isSufficient);

            return isSufficient;
        } catch (NumberFormatException e) {
            LOG.error("Invalid number format: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid number format for number of guests");
        } catch (Exception e) {
            LOG.error("Error checking table capacity: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking table capacity", e);
        }
    }

    /**
     * Get the table ID for a table number at a specific location
     * @param locationId The location ID
     * @param tableNumber The table number
     * @return The table ID, or null if the table doesn't exist
     */
    public String getTableId(String locationId, String tableNumber) {
        try {
            LOG.info("Getting table ID for table {} at location {}", tableNumber, locationId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":tableNumber", AttributeValue.builder().s(tableNumber).build());

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(diningTableName)
                    .filterExpression("locationId = :locationId AND tableNumber = :tableNumber")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            if (response.items().isEmpty()) {
                LOG.warn("Table {} not found at location {}", tableNumber, locationId);
                return null;
            }

            // Get the table ID from the first (and should be only) item
            Map<String, AttributeValue> item = response.items().get(0);
            if (!item.containsKey("tableId")) {
                LOG.warn("Table ID not found for table {} at location {}", tableNumber, locationId);
                return null;
            }

            String tableId = item.get("tableId").s();
            LOG.info("Table {} at location {} has ID {}", tableNumber, locationId, tableId);
            return tableId;
        } catch (Exception e) {
            LOG.error("Error getting table ID: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting table ID", e);
        }
    }

    /**
     * Check if a table is already booked for a specific date and time slot
     * @param locationId The location ID
     * @param tableId The table ID
     * @param date The date of booking
     * @param slotId The time slot ID
     * @return true if the table is already booked, false otherwise
     */
    public boolean isTableAlreadyBooked(String locationId, String tableId, String date, String slotId) {
        try {
            LOG.info("Checking if table {} at location {} is already booked for date {} and slot {}",
                    tableId, locationId, date, slotId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":tableId", AttributeValue.builder().s(tableId).build());
            expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());
            expressionAttributeValues.put(":slotId", AttributeValue.builder().s(slotId).build());
            expressionAttributeValues.put(":status", AttributeValue.builder().s("Reserved").build());

            // Create expression attribute names for reserved keywords
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#date", "date");  // Use #date to refer to the reserved keyword 'date'
            expressionAttributeNames.put("#status", "status");  // Also use for status just to be safe

            // Create scan request with filter and expression attribute names
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(bookingTableName)
                    .filterExpression("locationId = :locationId AND tableId = :tableId AND #date = :date AND slotId = :slotId AND #status = :status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            boolean isBooked = !response.items().isEmpty();
            LOG.info("Table {} at location {} for date {} and slot {} is already booked: {}",
                    tableId, locationId, date, slotId, isBooked);

            return isBooked;
        } catch (Exception e) {
            LOG.error("Error checking if table is already booked: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking booking availability", e);
        }
    }

    public List<String> getWaiterEmails() {
        try {
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(waiterTableName)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<String> waiterEmails = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                waiterEmails.add(item.get("email").s());
            }

            return waiterEmails;
        } catch (Exception e) {
            LOG.error("Error getting waiters: {}", e.getMessage());
            throw new RuntimeException("Error getting waiters", e);
        }
    }

    public String getLocationAddress(String locationId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            //according to Mehak's code
            LOG.info("Mehak's loc id {}",locationId);
            key.put("locationId", AttributeValue.builder().s(locationId).build());
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(locationTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item().isEmpty()) {
                return "Unknown Location";
            }

            return response.item().get("address").s();
        } catch (Exception e) {
            LOG.error("Error getting location address: {}", e.getMessage());
            return "Unknown Location";
        }
    }

    /**
     * Get all waiters for a specific location
     * @param locationId The location ID
     * @return List of waiter emails for the location
     */
    public List<String> getWaiterEmailsByLocation(String locationId) {
        try {
            LOG.info("Getting waiters for location: {}", locationId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(waiterTableName)
                    .filterExpression("locationId = :locationId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            List<String> waiterEmails = new ArrayList<>();

            for (Map<String, AttributeValue> item : response.items()) {
                waiterEmails.add(item.get("email").s());
            }

            LOG.info("Found {} waiters for location {}", waiterEmails.size(), locationId);
            return waiterEmails;
        } catch (Exception e) {
            LOG.error("Error getting waiters by location: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting waiters by location", e);
        }
    }

    /**
     * Count the number of bookings for a waiter on a specific date
     * @param waiterEmail The waiter's email
     * @param date The date to check
     * @return The number of bookings
     */
    public int countWaiterBookingsForDate(String waiterEmail, String date) {
        try {
            LOG.info("Counting bookings for waiter {} on date {}", waiterEmail, date);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":waiterEmail", AttributeValue.builder().s(waiterEmail).build());
            expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());
            expressionAttributeValues.put(":status", AttributeValue.builder().s("Reserved").build());

            // Create expression attribute names for reserved keywords
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#date", "date");
            expressionAttributeNames.put("#status", "status");

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(bookingTableName)
                    .filterExpression("waiterEmail = :waiterEmail AND #date = :date AND #status = :status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            int bookingCount = response.items().size();

            LOG.info("Waiter {} has {} bookings on date {}", waiterEmail, bookingCount, date);
            return bookingCount;
        } catch (Exception e) {
            LOG.error("Error counting waiter bookings: {}", e.getMessage(), e);
            throw new RuntimeException("Error counting waiter bookings", e);
        }
    }

    /**
     * Count the number of tables assigned to a waiter for a specific date and time slot
     * @param waiterEmail The waiter's email
     * @param date The date to check
     * @param slotId The time slot ID
     * @return The number of tables assigned
     */
    public int countWaiterTablesForSlot(String waiterEmail, String date, String slotId) {
        try {
            LOG.info("Counting tables for waiter {} on date {} for slot {}",
                    waiterEmail, date, slotId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":waiterEmail", AttributeValue.builder().s(waiterEmail).build());
            expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());
            expressionAttributeValues.put(":slotId", AttributeValue.builder().s(slotId).build());
            expressionAttributeValues.put(":status", AttributeValue.builder().s("Reserved").build());

            // Create expression attribute names for reserved keywords
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#date", "date");
            expressionAttributeNames.put("#status", "status");

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(bookingTableName)
                    .filterExpression("waiterEmail = :waiterEmail AND #date = :date AND slotId = :slotId AND #status = :status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            int tableCount = response.items().size();

            LOG.info("Waiter {} has {} tables assigned for date {} and slot {}",
                    waiterEmail, tableCount, date, slotId);

            return tableCount;
        } catch (Exception e) {
            LOG.error("Error counting waiter tables: {}", e.getMessage(), e);
            throw new RuntimeException("Error counting waiter tables", e);
        }
    }

    /**
     * Check if a waiter has reached the maximum number of tables for a specific date and time slot
     * @param waiterEmail The waiter's email
     * @param date The date to check
     * @param slotId The time slot ID
     * @return true if the waiter has reached the maximum, false otherwise
     */
    public boolean hasWaiterReachedMaxTables(String waiterEmail, String date, String slotId) {
        int tableCount = countWaiterTablesForSlot(waiterEmail, date, slotId);
        boolean hasReachedMax = tableCount >= MAX_TABLES_PER_WAITER;

        LOG.info("Waiter {} has {} tables assigned for date {} and slot {}. Max reached: {}",
                waiterEmail, tableCount, date, slotId, hasReachedMax);

        return hasReachedMax;
    }

    /**
     * Check if a waiter is already booked for a specific date and time slot
     * This method is kept for backward compatibility but is no longer used in the main flow
     * @param waiterEmail The waiter's email
     * @param date The date to check
     * @param slotId The time slot ID
     * @return true if the waiter is already booked, false otherwise
     */
    public boolean isWaiterAlreadyBookedForSlot(String waiterEmail, String date, String slotId) {
        try {
            LOG.info("Checking if waiter {} is already booked for date {} and slot {}",
                    waiterEmail, date, slotId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":waiterEmail", AttributeValue.builder().s(waiterEmail).build());
            expressionAttributeValues.put(":date", AttributeValue.builder().s(date).build());
            expressionAttributeValues.put(":slotId", AttributeValue.builder().s(slotId).build());
            expressionAttributeValues.put(":status", AttributeValue.builder().s("Reserved").build());

            // Create expression attribute names for reserved keywords
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#date", "date");
            expressionAttributeNames.put("#status", "status");

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(bookingTableName)
                    .filterExpression("waiterEmail = :waiterEmail AND #date = :date AND slotId = :slotId AND #status = :status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);
            boolean isBooked = !response.items().isEmpty();

            LOG.info("Waiter {} is {} booked for date {} and slot {}",
                    waiterEmail, isBooked ? "already" : "not", date, slotId);

            return isBooked;
        } catch (Exception e) {
            LOG.error("Error checking if waiter is already booked: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking waiter availability", e);
        }
    }

    /**
     * Find an available waiter for a specific date, time slot, and location
     * @param locationId The location ID
     * @param date The date
     * @param slotId The time slot ID
     * @return The email of an available waiter, or null if none available
     */
    public String findAvailableWaiterForSlot(String locationId, String date, String slotId) {
        try {
            LOG.info("Finding available waiter for location {} on date {} for slot {}",
                    locationId, date, slotId);

            // Get all waiters for the location
            List<String> waiters = getWaiterEmailsByLocation(locationId);

            if (waiters.isEmpty()) {
                LOG.warn("No waiters available for location: {}", locationId);
                return null;
            }

            // Create a map to store waiter booking counts (only for waiters who haven't reached max tables)
            Map<String, Integer> availableWaiterBookingCounts = new HashMap<>();

            // Check each waiter's availability for this slot
            for (String waiterEmail : waiters) {
                // Skip waiters who have reached the maximum number of tables
                if (hasWaiterReachedMaxTables(waiterEmail, date, slotId)) {
                    LOG.info("Waiter {} has reached the maximum number of tables for slot {} on {}",
                            waiterEmail, slotId, date);
                    continue;
                }

                // Count total bookings for the day for available waiters
                int bookingCount = countWaiterBookingsForDate(waiterEmail, date);
                availableWaiterBookingCounts.put(waiterEmail, bookingCount);
            }

            if (availableWaiterBookingCounts.isEmpty()) {
                LOG.warn("No available waiters for location {} on date {} for slot {}. All waiters have reached their maximum table limit.",
                        locationId, date, slotId);
                return null;
            }

            // Find the waiter with the minimum number of bookings among available waiters
            String selectedWaiter = availableWaiterBookingCounts.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

            LOG.info("Selected waiter {} for location {} on date {} for slot {}",
                    selectedWaiter, locationId, date, slotId);

            return selectedWaiter;
        } catch (Exception e) {
            LOG.error("Error finding available waiter: {}", e.getMessage(), e);
            throw new RuntimeException("Error finding available waiter", e);
        }
    }

    /**
     * Get a booking by ID
     * @param reservationId The reservation ID
     * @return The booking or null if not found
     */
    public Booking getBookingById(String reservationId) {
        try {
            LOG.info("Getting booking with ID: {}", reservationId);

            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", AttributeValue.builder().s(reservationId).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(bookingTableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);

            if (response.item().isEmpty()) {
                LOG.warn("Booking not found with ID: {}", reservationId);
                return null;
            }

            // Convert DynamoDB item to Booking object
            Booking booking = new Booking();
            booking.setReservationId(response.item().get("reservationId").s());
            booking.setTableId(response.item().get("tableId").s());
            booking.setCustomerEmail(response.item().get("customerEmail").s());
            booking.setSlotId(response.item().get("slotId").s());
            booking.setDate(response.item().get("date").s());
            booking.setWaiterEmail(response.item().get("waiterEmail").s());
            booking.setStatus(response.item().get("status").s());
            booking.setLocationId(response.item().get("locationId").s());
            booking.setNumberOfGuests(response.item().get("numberOfGuests").s());

            if (response.item().containsKey("secretCode")) {
                booking.setSecretCode(response.item().get("secretCode").s());
            }

            LOG.info("Found booking with ID: {}", reservationId);
            return booking;
        } catch (Exception e) {
            LOG.error("Error getting booking by ID: {}", e.getMessage(), e);
            return null;
        }
    }


    /**
     * Get the table number for a table ID at a specific location
     * @param locationId The location ID
     * @param tableId The table ID
     * @return The table number, or null if the table doesn't exist
     */
    public String getTableNumber(String locationId, String tableId) {
        try {
            LOG.info("Getting table number for table ID {} at location {}", tableId, locationId);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":tableId", AttributeValue.builder().s(tableId).build());

            // Create scan request with filter
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(diningTableName)
                    .filterExpression("locationId = :locationId AND tableId = :tableId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            if (response.items().isEmpty()) {
                LOG.warn("Table with ID {} not found at location {}", tableId, locationId);
                return null;
            }

            // Get the table number from the first (and should be only) item
            Map<String, AttributeValue> item = response.items().get(0);
            if (!item.containsKey("tableNumber")) {
                LOG.warn("Table number not found for table ID {} at location {}", tableId, locationId);
                return null;
            }

            String tableNumber = item.get("tableNumber").s();
            LOG.info("Table with ID {} at location {} has number {}", tableId, locationId, tableNumber);
            return tableNumber;
        } catch (Exception e) {
            LOG.error("Error getting table number: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting table number", e);
        }
    }
}