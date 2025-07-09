package com.restaurantapp.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.*;
import com.restaurantapp.Model.CustomerFeedback;
import com.restaurantapp.Model.CustomerFeedbackRequest;
import com.restaurantapp.Model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomerFeedbackRepository {
    private final Logger LOG = LoggerFactory.getLogger(CustomerFeedbackRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;
    private String finalFeedbackId="";
    private final String bookingTable = System.getenv("booking_table");
    private final String feedbackTable = System.getenv("feedback_table");

    public CustomerFeedbackRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Reservation checkForReservation(CustomerFeedbackRequest request,String customerEmail , String method) {
       try {
           String reservationId = request.getReservationId();
           Map<String, AttributeValue> key = new HashMap<>();
           key.put("reservationId", AttributeValue.builder().s(reservationId).build());

           GetItemRequest getPaticularReservation = GetItemRequest.builder()
                   .tableName(bookingTable)
                   .key(key)
                   .build();

           GetItemResponse getItemResponse = dynamoDbClient.getItem(getPaticularReservation);

           if (getItemResponse.item().isEmpty()) {
               throw new BadRequestException("Reservation Not Found ");
           }

           if (!customerEmail.equals(getItemResponse.item().get("customerEmail").s())) {
               throw new UnauthorizedException("You are not allowed to Add this feedback");
           }


           if (getItemResponse.item().get("status").s().equals("Cancelled")){
               throw new ConflictException("You are not allowed to provide feedback before cancelled reservations");
           }

           if (!getItemResponse.item().get("status").s().equals("In Progress") && method.equals("create")) {
               throw new ForbiddenException("You are not allowed to provide feedback before reservation timings");
           }
           if (!getItemResponse.item().get("status").s().equals("Finished") && method.equals("update")) {
               throw new ConflictException("You are not allowed to Update feedback before reservation is finished");
           }



           Reservation r = new Reservation();
           r.setCustomerEmail(customerEmail);
           r.setWaiterEmail(getItemResponse.item().get("waiterEmail").s());
           r.setReservationId(reservationId);
           r.setLocationId(getItemResponse.item().get("locationId").s());

           return r;


       }catch (ForbiddenException e) {
           LOG.info("Exception occured in checkForReservation");
           throw e;
       }catch (BadRequestException e) {
           LOG.info("Exception occured in checkForReservation");
           throw e;
       }catch (ConflictException e) {
           LOG.info("Exception occured in checkForReservation");
           throw e;
       }catch (Exception e) {
           LOG.info("Exception occured in checkForReservation");
           throw e;
       }
    }


    public String updateTheFeedback(CustomerFeedback customerFeedback){
        try {
            boolean feedbackExists = feedbackExists(customerFeedback.getReservationId(), customerFeedback.getType());
            if (feedbackExists) {
                //update

                //Get values
                String comment = customerFeedback.getComment();
                String rating = customerFeedback.getRating();
                String feedbackId = finalFeedbackId;

                // Create update expression parts
                StringBuilder updateExpression = new StringBuilder("SET ");
                Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
                Map<String, String> expressionAttributeNames = new HashMap<>();

                boolean hasUpdates = false;

                // Add comment update if provided
                if (comment != null && !comment.trim().isEmpty()) {
                    updateExpression.append("#comment = :comment");
                    expressionAttributeValues.put(":comment", AttributeValue.builder().s(comment).build());
                    expressionAttributeNames.put("#comment", "comment");
                    hasUpdates = true;
                }

                // Add rating update if provided
                if (rating != null && !rating.trim().isEmpty()) {
                    if (hasUpdates) {
                        updateExpression.append(", ");
                    }
                    updateExpression.append("#rating = :rating");
                    expressionAttributeValues.put(":rating", AttributeValue.builder().s(rating).build());
                    expressionAttributeNames.put("#rating", "rating");
                    hasUpdates = true;
                }

                // Add date update
                if (hasUpdates) {
                    updateExpression.append(", #date = :date");
                    expressionAttributeValues.put(":date", AttributeValue.builder().s(LocalDate.now().toString()).build());
                    expressionAttributeNames.put("#date", "date");
                }

                // If no updates, return early
                if (!hasUpdates) {
                    LOG.info("No updates provided for feedback with ID: {}", feedbackId);
                    return null;
                }

                // Create the key map
                Map<String, AttributeValue> key = new HashMap<>();
                key.put("feedbackId", AttributeValue.builder().s(feedbackId).build());

                // Create the update request
                UpdateItemRequest request = UpdateItemRequest.builder()
                        .tableName(feedbackTable)
                        .key(key)
                        .updateExpression(updateExpression.toString())
                        .expressionAttributeValues(expressionAttributeValues)
                        .expressionAttributeNames(expressionAttributeNames)
                        .build();

                // Execute the request
                UpdateItemResponse response = dynamoDbClient.updateItem(request);

                LOG.info("Successfully updated feedback with ID: {}", finalFeedbackId);

                return finalFeedbackId;


            }else{
                return "Feedback Not Found";
            }
        }catch (Exception e) {
            LOG.info("Exception occured in updateFeedback");
            throw e;
        }
    }



    public String createFeedback(CustomerFeedback customerFromDB) {
        try {

            boolean feedbackExists = feedbackExists(customerFromDB.getReservationId(), customerFromDB.getType());

            String feedBackId;

            if (!feedbackExists) {
                // Feedback doesn't exist, create a new one
                feedBackId = customerFromDB.getFeedbackId();

            //add value to feedback
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("feedbackId", AttributeValue.builder().s(feedBackId).build());
            item.put("locationId", AttributeValue.builder().s(customerFromDB.getLocationId()).build());
            item.put("reservationId", AttributeValue.builder().s(customerFromDB.getReservationId()).build());
            item.put("rating", AttributeValue.builder().s(customerFromDB.getRating()).build());
            item.put("type", AttributeValue.builder().s(customerFromDB.getType()).build());
            item.put("comment", AttributeValue.builder().s(customerFromDB.getComment()).build());
            item.put("date",AttributeValue.builder().s(LocalDate.now().toString()).build());
            item.put("customerEmail", AttributeValue.builder().s(customerFromDB.getCustomerEmail()).build());
            item.put("waiterEmail", AttributeValue.builder().s(customerFromDB.getWaiterEmail()).build());

            PutItemRequest addItem = PutItemRequest.builder()
                    .tableName(feedbackTable)
                    .item(item)
                    .build();

            PutItemResponse addItemResponse = dynamoDbClient.putItem(addItem);

            return customerFromDB.getFeedbackId();}
            else {
                return "Feedback already exists";
            }
        }
        catch (Exception e) {
            LOG.info("Exception occured in createFeedback");
            throw e;
        }

    }

    /**
     * Check if feedback already exists for a reservation ID and type
     * @param reservationId The reservation ID
     * @param type The feedback type
     * @return true if feedback exists, false otherwise
     */
    public boolean feedbackExists(String reservationId, String type) {
        try {
            LOG.info("Checking if feedback exists for reservation: {} and type: {}", reservationId, type);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":reservationId", AttributeValue.builder().s(reservationId).build());
            expressionAttributeValues.put(":typeAttr", AttributeValue.builder().s(type).build());

            // Create expression attribute names to handle reserved keywords
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#typeAttr", "type");

            // Initialize variables for pagination
            Map<String, AttributeValue> lastEvaluatedKey = null;
            boolean exists = false;

            do {
                // Create scan request with filter
                ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                        .tableName(feedbackTable)
                        .filterExpression("reservationId = :reservationId AND #typeAttr = :typeAttr")
                        .expressionAttributeValues(expressionAttributeValues)
                        .expressionAttributeNames(expressionAttributeNames)
                        .limit(1); // We only need to know if any feedback exists

                // Add exclusiveStartKey for pagination if we have a lastEvaluatedKey
                if (lastEvaluatedKey != null) {
                    scanRequestBuilder.exclusiveStartKey(lastEvaluatedKey);
                }

                ScanRequest scanRequest = scanRequestBuilder.build();
                ScanResponse response = dynamoDbClient.scan(scanRequest);

                LOG.info("Scan Response: {}", response);

                // Check if we found any matching items
                if (response.count() > 0) {
                    finalFeedbackId = response.items().get(0).get("feedbackId").s();
                    exists = true;
                    break; // We found a match, no need to continue scanning
                }

                // Update lastEvaluatedKey for the next iteration
                lastEvaluatedKey = response.lastEvaluatedKey();

                // Fix: Check if lastEvaluatedKey is null or empty
            } while (lastEvaluatedKey != null && !lastEvaluatedKey.isEmpty());

            LOG.info("Feedback for reservation {} and type {} exists: {}", reservationId, type, exists);
            return exists;
        } catch (Exception e) {
            LOG.error("Error checking if feedback exists: {}", e.getMessage(), e);
            throw new RuntimeException("Error checking if feedback exists", e);
        }
    }


    public ScanResponse getAllReservations(String reservationId) {

        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":reservationId", AttributeValue.builder().s(reservationId).build());

        ScanRequest scanRequest = ScanRequest.builder()
                .tableName(feedbackTable)
                .filterExpression("reservationId = :reservationId")
                .expressionAttributeValues(expressionValues).build();

        ScanResponse scanResponse = dynamoDbClient.scan(scanRequest);
        LOG.info("Scan Response: {}", scanResponse);



        return scanResponse;

    }

    public boolean checkReservationForEmail(String reservationId, String customerEmail) {

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("reservationId", AttributeValue.builder().s(reservationId).build());

            GetItemRequest scanRequest = GetItemRequest.builder()
                    .tableName(bookingTable)
                    .key(key)
                    .build();

            GetItemResponse itemResponse = dynamoDbClient.getItem(scanRequest);

            if (itemResponse.item() == null || itemResponse.item().size()<=0){
                throw new Exception("Reservation Not found");
            }

            if (itemResponse.item().get("customerEmail").s().equals(customerEmail)) {
                return true;
            }

            return false;
        }
        catch (Exception e) {
            LOG.info("Exception occurred in checkReservationForEmail");
            throw new BadRequestException("Exception occurred in passing ReservationId");
        }
    }
}
