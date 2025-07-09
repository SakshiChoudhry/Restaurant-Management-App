package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantapp.Model.FeedbackPageResponse;
import com.restaurantapp.Service.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FeedbackController {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackController.class);
    private final FeedbackService feedbackService;


    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;

    }

    public APIGatewayProxyResponseEvent getFeedback(Map<String, String> pathParameters, Map<String, String> queryParameters) {

        LOG.info("Processing feedback request");

        // Extract path parameters
        String locationId = pathParameters.get("id");
        if (locationId == null || locationId.isEmpty()) {
            return ApiResponse.error("Location ID is required");
        }
        // Extract query parameters
        String type = queryParameters.get("type");
        if (type == null || type.isEmpty()) {
            return ApiResponse.error("Feedback type is required");
        }

        // Parse pagination parameters
        int page = parseIntParameter(queryParameters.get("page"), 0);
        int size = parseIntParameter(queryParameters.get("size"), 20);

        // Parse sort parameters
        List<String> sort = queryParameters.containsKey("sort") ?
                Arrays.asList(queryParameters.get("sort").split(",")) :
                Collections.emptyList();

        try {

            Set<String> allowed=Set.of("type","sort","page","size");
            if(!allowed.containsAll(queryParameters.keySet()))
            {
                return ApiResponse.error("Invalid Query Parameters");
            }
            else if(page<0)
                return ApiResponse.error("Page cannot be less than 0");
            else if(size<0)
                return ApiResponse.error("Size cannot be less than 0");

            // Get feedback from service
            FeedbackPageResponse response = feedbackService.getFeedbackByLocationAndType(locationId, type, page, size, sort);

            // Return the response directly
            if(response.getTotalElements()==-1)
                return ApiResponse.notFound("Location does not exist");
            else if(response.getTotalElements()==-2)
                return ApiResponse.notFound("Feedback Type Invalid");
            else
                return ApiResponse.success(response);

        } catch (Exception e) {
            LOG.error("Error processing feedback request", e);
            return ApiResponse.serverError("Error retrieving feedback: " + e.getMessage());
        }
    }

    private int parseIntParameter(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
