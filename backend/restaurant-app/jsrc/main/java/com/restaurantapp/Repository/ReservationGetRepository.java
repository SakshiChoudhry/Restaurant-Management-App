package com.restaurantapp.Repository;

import com.restaurantapp.Model.Location;
import com.restaurantapp.Model.Reservation;
import com.restaurantapp.Model.ReservationGet;
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
public class ReservationGetRepository
{
    private static final Logger LOG = LoggerFactory.getLogger(ReservationGetRepository.class);

    private final DynamoDbClient dynamoDbClient;
    private final String reservationTableName;
    private final String locationTableName;
    private final String waiterTable = System.getenv("waiter_table");
    private final String orderTable=System.getenv("order_table");
    private final String userTable=System.getenv("user_table");


    // Hardcoded slot map
    private final Map<String, Slot> slotMap;

    @Inject
    public ReservationGetRepository()
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

    public String findUserNameFromEmail(String customerEmail) {
        Map<String,AttributeValue> expressionAttributeValues =new HashMap<>();
        expressionAttributeValues.put(":email", AttributeValue.builder().s(customerEmail).build());
        Map<String,String> expressionAttributeNames =new HashMap<>();
        expressionAttributeNames.put("#email","email");

        ScanRequest scanRequest=ScanRequest.builder()
                .tableName(userTable)
                .filterExpression("#email = :email")
                .expressionAttributeValues(expressionAttributeValues)
                .expressionAttributeNames(expressionAttributeNames)
                .build();
        ScanResponse scanResponse=dynamoDbClient.scan(scanRequest);
        if(scanResponse.items().isEmpty() || scanResponse.items()==null){
            return "User not found";
        }
        Map<String, AttributeValue> userItem=scanResponse.items().get(0);
        if(!userItem.containsKey("firstName")){
            throw new RuntimeException("User data is incomplete: first name not found");
        }
        String lastName="";
        if(userItem.containsKey("lastName")){
            lastName=userItem.get("lastName").s();
        }
        String firstName=userItem.get("firstName").s();

        return firstName+" "+lastName;
    }
    public List<ReservationGet> findReservationsByEmail(String email)
    {
        try {
            LOG.info("Finding reservations for email: {}", email);
            LOG.debug("Using reservation table: {}", reservationTableName);

            // Check if table name is available
            if (reservationTableName == null || reservationTableName.isEmpty()) {
                LOG.error("Reservation table name is null or empty");
                throw new RuntimeException("Reservation table name is not configured");
            }

            List<ReservationGet> reservations = new ArrayList<>();


            try {
                Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                expressionAttributeValues.put(":email", AttributeValue.builder().s(email).build());

                ScanRequest scanRequest = ScanRequest.builder()
                        .tableName(reservationTableName)
                        .filterExpression("waiterEmail = :email")
                        .expressionAttributeValues(expressionAttributeValues)
                        .build();

                ScanResponse response = dynamoDbClient.scan(scanRequest);

                for (Map<String, AttributeValue> item : response.items()) {
                    try {
                        LOG.info(item.toString(), "bdkajsbd");
                        ReservationGet reservation = mapToReservation(item);
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
            key.put("id", AttributeValue.builder().s(locationId).build());
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
            location.setLocationId(item.get("id").s());

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

    public String findNumberOfDishesOfOrder(String customerEmail) {
//        String orderId=findOrderId(reservationId);
//        String orderId="1";
        Map<String,AttributeValue> expressionAttributeValues =new HashMap<>();
        expressionAttributeValues.put(":customerEmail", AttributeValue.builder().s(customerEmail).build());
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#email", "customerEmail");
//        log.info("order table is {}",orderTable);
        ScanRequest scanRequest=ScanRequest.builder()
                .tableName(orderTable)
                .filterExpression("#email = :customerEmail")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();
        ScanResponse scanResponse=dynamoDbClient.scan(scanRequest);
        if(scanResponse.items()==null || scanResponse.items().isEmpty()){
            return "No orders found";
        }
        Map<String, AttributeValue> item=scanResponse.items().get(0);
        if (!item.containsKey("dishItems")) {
//            log.warn("list of dishes not found: {}", customerEmail);
            throw new RuntimeException("Reservation data incomplete:  not found");
        }
        List<AttributeValue> dishesOfOrderId=item.get("dishItems").l();
        return String.valueOf(dishesOfOrderId.size());
    }
    private ReservationGet mapToReservation(Map<String, AttributeValue> item)
    {
        ReservationGet reservation = new ReservationGet();

        try {
            // Handle ReservationId
            if (item.containsKey("reservationId")) {
                reservation.setReservationId(item.get("reservationId").s());
            }

            // Handle TableId
            if (item.containsKey("tableId")) {
                reservation.setTableNumber(item.get("tableId").s());
            }

            if (item.containsKey("customerEmail"))
            {
                reservation.setCustomerEmail(item.get("customerEmail").s());
            }

            if (item.containsKey("slotId")) {
                reservation.setSlotId(item.get("slotId").s());
            }

            if (item.containsKey("waiterEmail")) {
                reservation.setWaiterEmail(item.get("waiterEmail").s());
            }

            if (item.containsKey("status")) {
                reservation.setStatus(item.get("status").s());
            }

//            String locationId=findLocationIdFromWaiterTable(item.get("waiterEmail").s());
//            String locationAddress=getLocationAddress(locationId);
//
//            reservation.setLocationAddress(locationAddress);

            if (item.containsKey("date")) {
                reservation.setDate(item.get("date").s());
            }

            if (item.containsKey("numberOfGuests"))
            {
                AttributeValue guestsValue = item.get("numberOfGuests");
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
    public String findLocationIdFromWaiterTable(String waiterEmail){
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#email", "email");

        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":email", AttributeValue.builder().s(waiterEmail).build());

        // Create a scan request to find the waiter by email
        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(waiterTable)
                .filterExpression("#email = :email")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        // Execute the scan
        ScanResponse response = dynamoDbClient.scan(scanRequest);

        // Check if the waiter was found
        if (response.items() == null || response.items().isEmpty()) {
            throw new RuntimeException("Waiter not found with email: " + waiterEmail);
        }

        // Get the first (and should be only) item from the result
        Map<String, AttributeValue> waiterItem = response.items().get(0);

        // Check if the waiter has a location assigned
        if (!waiterItem.containsKey("locationId")) {
            throw new RuntimeException("Waiter has no location assigned: " + waiterEmail);
        }

        // Return the location ID
        return waiterItem.get("locationId").s();
    }

    public String getLocationAddress(String locationId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
//            log.info("loc id {}",locationId);
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
//            log.error("Error getting location address: {}", e.getMessage());
            return "Unknown Location";
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
