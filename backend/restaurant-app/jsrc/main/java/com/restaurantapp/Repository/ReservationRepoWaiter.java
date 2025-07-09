package com.restaurantapp.Repository;

import com.restaurantapp.Controller.ReservationWaiterController;
import com.restaurantapp.Model.Booking;
import com.restaurantapp.Model.BookingWaiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReservationRepoWaiter {
    private final DynamoDbClient dynamoDbClient;
    private final String bookingTable = System.getenv("booking_table");
    private final String waiterTable = System.getenv("waiter_table");
    private final String locationTableName = System.getenv("location_table");
    private final String userTable=System.getenv("user_table");
    private final String orderTable=System.getenv("order_table");
    private final String diningTableName=System.getenv("tables_table_name");
    private static final Logger log = LoggerFactory.getLogger(ReservationWaiterController.class);
//environment variable -> properties file, Configuration files application.properties, application.yaml,yml
    @Inject
    public ReservationRepoWaiter() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
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
            throw new IllegalArgumentException("Forbidden: Only waiters are authorized to create bookings using this API.");
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

    public String getTableId(String locationId, String tableNumber) {
        try {
            log.info("Getting table ID for table {} at location {}", tableNumber, locationId);

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
                log.warn("Table {} not found at location {}", tableNumber, locationId);
                return null;
            }

            // Get the table ID from the first (and should be only) item
            Map<String, AttributeValue> item = response.items().get(0);
            if (!item.containsKey("tableId")) {
                log.warn("Table ID not found for table {} at location {}", tableNumber, locationId);
                return null;
            }

            String tableId = item.get("tableId").s();
            log.info("Table {} at location {} has ID {}", tableNumber, locationId, tableId);
            return tableId;
        } catch (Exception e) {
            log.error("Error getting table ID: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting table ID", e);
        }
    }
    public boolean isTableAlreadyBooked(String locationId, String tableId, String date, String slotId) {
        try {
            log.info("Checking if table {} at location {} is already booked for date {} and slot {}",
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
                    .tableName(bookingTable)
                    .filterExpression("locationId = :locationId AND tableId = :tableId AND #date = :date AND slotId = :slotId AND #status = :status")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            boolean isBooked = !response.items().isEmpty();
            log.info("Table {} at location {} for date {} and slot {} is already booked: {}",
                    tableId, locationId, date, slotId, isBooked);

            return isBooked;
        } catch (Exception e) {
            log.error("Error checking if table is already booked: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking booking availability", e);
        }
    }
    public BookingWaiter createBooking(BookingWaiter booking) {
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
            item.put("locationId", AttributeValue.builder().s(booking.getLocationId()).build());
            item.put("numberOfGuests", AttributeValue.builder().s(booking.getNumberOfGuests()).build());
//            item.put("feedbackId", AttributeValue.builder().s(booking.getFeedbackId()).build());

            // Create request
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(bookingTable)
                    .item(item)
                    .build();

            // Execute request
            dynamoDbClient.putItem(request);
            log.info("Booking created successfully: {}", booking.getReservationId());

            return booking;
        } catch (Exception e) {
            log.error("Error creating booking: {}", e.getMessage());
            throw new RuntimeException("Error creating booking", e);
        }
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
            throw new IllegalArgumentException("User not found with email id "+customerEmail);
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

    public String findOrderId(String reservationId){
        Map<String,AttributeValue> key =new HashMap<>();
        key.put(":reservationId", AttributeValue.builder().s(reservationId).build());

        GetItemRequest getItemRequest=GetItemRequest.builder()
                .tableName(bookingTable)
                .key(key)
                .build();
        GetItemResponse getItemResponse=dynamoDbClient.getItem(getItemRequest);
        if(getItemResponse.item()==null || getItemResponse.item().isEmpty()){
            throw new IllegalArgumentException("Reservation not found with id: " + reservationId);
        }
        if (!getItemResponse.item().containsKey("orderId")) {
            log.warn("Reservation found but orderId attribute is missing for reservationId: {}", reservationId);
            throw new RuntimeException("Reservation data incomplete: orderId not found");
        }
        String orderid=getItemResponse.item().get("orderId").s();
        return orderid;
    }
    public String findNumberOfDishesOfOrder(String customerEmail) {
//        String orderId=findOrderId(reservationId);
//        String orderId="1";
        Map<String,AttributeValue> expressionAttributeValues =new HashMap<>();
        expressionAttributeValues.put(":customerEmail", AttributeValue.builder().s(customerEmail).build());
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#email", "customerEmail");
        log.info("order table is {}",orderTable);
        ScanRequest scanRequest=ScanRequest.builder()
                .tableName(orderTable)
                .filterExpression("#email = :customerEmail")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();
        ScanResponse scanResponse=dynamoDbClient.scan(scanRequest);
        if(scanResponse.items()==null || scanResponse.items().isEmpty()){
            return String.valueOf(0);
        }
        Map<String, AttributeValue> item=scanResponse.items().get(0);
        if (!item.containsKey("dishItems")) {
            log.warn("list of dishes not found: {}", customerEmail);
            throw new RuntimeException("Reservation data incomplete:  not found");
        }
        List<AttributeValue> dishesOfOrderId=item.get("dishItems").l();
        return String.valueOf(dishesOfOrderId.size());
    }
    public String findStatusOfOrder(String customerEmail) {
        log.info("entering in findStatusOfOrder");
        Map<String,AttributeValue> expressionAttributeValues =new HashMap<>();
        expressionAttributeValues.put(":customerEmail", AttributeValue.builder().s(customerEmail).build());
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#email", "customerEmail");
        log.info("order table is {}",orderTable);
        ScanRequest scanRequest=ScanRequest.builder()
                .tableName(orderTable)
                .filterExpression("#email = :customerEmail")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        log.info("past scan request {}",scanRequest);
        ScanResponse scanResponse=dynamoDbClient.scan(scanRequest);
        if(scanResponse.items()==null || scanResponse.items().isEmpty()){
            throw new IllegalArgumentException("No status found for customer email: " + customerEmail);
        }
        log.info("past scan response {}",scanResponse);
        Map<String, AttributeValue> item=scanResponse.items().get(0);
        if (!item.containsKey("state")) {
            log.warn("state not found: {}", customerEmail);
            throw new RuntimeException("Order data incomplete: state not found");
        }
        String state=item.get("state").s();
        log.info("state is {}",state);
        return state;
    }
    public String getLocationAddress(String locationId) {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            log.info("loc id {}",locationId);
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
            log.error("Error getting location address: {}", e.getMessage());
            return "Unknown Location";
        }
    }

    public String reservationRepoWaiter(String email, String slotId, String tableNumber,String locationId, String date) {
        try {
            // First, find the reservation with the given criteria
            String tableId = getTableId(locationId, tableNumber);

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":customerEmail", AttributeValue.builder().s(email).build());
            expressionAttributeValues.put(":tableId", AttributeValue.builder().s(tableId).build());
            log.info("before scan req");
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(bookingTable)
                    .filterExpression("customerEmail = :customerEmail AND tableId = :tableId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
            log.info("after scan req {}",scanRequest);
            ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
            log.info("after scan response {}",scanResponse);
            if (scanResponse.items().isEmpty()) {
                throw new RuntimeException("No reservation found for customer with email: " + email +
                        " on date: " + date);
            }

            // Update the found reservation
            Map<String, AttributeValue> item = scanResponse.items().get(0);
            String reservationId = item.get("reservationId").s();

            // Create key for the update operation
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", AttributeValue.builder().s(reservationId).build());

            // Create expression attribute values for the update
            Map<String, AttributeValue> updateExpressionValues = new HashMap<>();
            updateExpressionValues.put(":slotId", AttributeValue.builder().s(slotId).build());
//            updateExpressionValues.put(":tableId", AttributeValue.builder().s(tableNumber).build());

            // Create the update request
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(bookingTable)
                    .key(key)
                    .updateExpression("SET slotId = :slotId")
//                    .updateExpression("SET slotId = :slotId, tableId = :tableId")
                    .expressionAttributeValues(updateExpressionValues)
                    .build();

            // Execute the update
            dynamoDbClient.updateItem(updateRequest);
            return "Reservation Updated";


        } catch (Exception e) {
            return "Error updating reservation: " + e.getMessage();
        }
    }
}
