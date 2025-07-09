package com.restaurantapp.Repository;

import com.restaurantapp.Model.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class FeedbackRepository {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackRepository.class);
    private final DynamoDbClient dynamoDbClient;
    private final String tableName = System.getenv("feedback_table");
    private final String locationTablename=System.getenv("location_table");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    public FeedbackRepository() {
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1) // Change to your region
                .build();
    }

    public List<Feedback> getFeedbackByLocationAndType(String locationId, String type, int page, int size, List<String> sort) {
        try {
            LOG.info("Retrieving feedback for location: {} and type: {}", locationId, type);

            // Create expression attribute values
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":type", AttributeValue.builder().s(type).build());

            // Replace the query with a scan operation
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    // Use filter expressions instead of key condition expressions
                    .filterExpression("locationId = :locationId AND #type = :type")
                    .expressionAttributeNames(Map.of("#type", "type")) // 'type' is a reserved word in DynamoDB
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();

            // Execute scan
            ScanResponse response = dynamoDbClient.scan(scanRequest);

            // Convert DynamoDB items to Feedback objects
            List<Feedback> allFeedbacks = response.items().stream()
                    .map(this::mapToFeedback)
                    .collect(Collectors.toList());

            // Apply sorting if provided
            if (sort != null && !sort.isEmpty()) {
                applySorting(allFeedbacks, sort);
            }

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allFeedbacks.size());

            if (startIndex >= allFeedbacks.size()) {
                return Collections.emptyList();
            }

            return allFeedbacks.subList(startIndex, endIndex);

        } catch (Exception e) {
            LOG.error("Error retrieving feedback from DynamoDB", e);
            throw new RuntimeException("Failed to retrieve feedback", e);
        }
    }

    public long countFeedbackByLocationAndType(String locationId, String type) {
        try {
            // Create expression attribute values

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":locationId", AttributeValue.builder().s(locationId).build());
            expressionAttributeValues.put(":type", AttributeValue.builder().s(type).build());

            // Replace query with scan for counting
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("locationId = :locationId AND #type = :type")
                    .expressionAttributeNames(Map.of("#type", "type"))
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
            LOG.info("Retrieving feedback ");
            ScanRequest locationScanRequest = ScanRequest.builder()
                    .tableName(locationTablename)
                    .filterExpression("locationId = :locationId")
                    .expressionAttributeValues(Map.of(":locationId", AttributeValue.builder().s(locationId).build()))
                    .build();
            LOG.info("Retrieving feedback {}", locationScanRequest.toString());
            // Execute scan
            ScanResponse response = dynamoDbClient.scan(scanRequest);
            ScanResponse locationResponse=dynamoDbClient.scan(locationScanRequest);
            if(locationResponse.count()==0)
                return -1;
            else if(!(type.equalsIgnoreCase("SERVICE_EXPERIENCE")||type.equalsIgnoreCase("CUISINE_EXPERIENCE")))
                return -2;
            else
                return response.count();

        } catch (Exception e) {
            LOG.error("Error counting feedback from DynamoDB", e);
            throw new RuntimeException("Failed to count feedback", e);
        }
    }

    private Feedback mapToFeedback(Map<String, AttributeValue> item) {
        Feedback feedback = new Feedback();
        feedback.setId(item.get("feedbackId").s());
        feedback.setRate(item.get("rating").s());
        feedback.setComment(item.get("comment").s());
        feedback.setUserName(item.get("customerEmail").s());
        feedback.setUserAvatarUrl(item.getOrDefault("imageUrl", AttributeValue.builder().s("").build()).s());
        feedback.setDate(item.get("date").s());
        feedback.setType(item.get("type").s());
        feedback.setLocationId(item.get("locationId").s());
        return feedback;
    }

    private void applySorting(List<Feedback> feedbacks, List<String> sortCriteria) {
        Comparator<Feedback> finalComparator = null;
            String property = sortCriteria.get(0);
            boolean ascending = sortCriteria.size() == 1 || "asc".equalsIgnoreCase(sortCriteria.get(1));
          String criteria="asc";
           Comparator<Feedback> comparator = createComparator(property);
            if (!ascending) {
                comparator = comparator.reversed();
                criteria="desc";
            }
            if (finalComparator == null) {
                finalComparator = comparator;
            } else {
                finalComparator = finalComparator.thenComparing(comparator);
            }



        if (finalComparator != null) {
            LOG.info("Sorting feedback list with {} criteria: {} , {}", sortCriteria.size(),sortCriteria.get(0),criteria);
            feedbacks.sort(finalComparator);
        }
    }

    private Comparator<Feedback> createComparator(String property) {

        LOG.info("Sorting feedback list with {} criteria", property);
        switch (property.toLowerCase()) {

            case "date":

                return Comparator.comparing(feedback -> parseDate(feedback.getDate()));

            case "rate":

                return Comparator.comparing(feedback -> {

                    try {

                        return Integer.parseInt(feedback.getRate());

                    } catch (NumberFormatException e) {

                        LOG.warn("Invalid rate value: {}", feedback.getRate());

                        return 0;

                    }

                });

            case "username":

                return Comparator.comparing(Feedback::getUserName,

                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

            default:

                LOG.warn("Unknown sort property: {}, defaulting to ID", property);

                return Comparator.comparing(Feedback::getId);

        }

    }

    private LocalDateTime parseDate(String dateStr) {

        try {

            return LocalDateTime.parse(dateStr, DATE_FORMATTER);

        } catch (DateTimeParseException e) {

            LOG.warn("Failed to parse date: {}", dateStr);

            return LocalDateTime.MIN; // Default value for unparseable dates

        }

    }

}
