package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating standardized API responses.
 */
public final class ApiResponse {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResponse.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, String> CORS_HEADERS = createCorsHeaders();

    private ApiResponse() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a successful response with the given data.
     *
     * @param data The data to include in the response
     * @return An API Gateway response with status code 200
     */
    public static APIGatewayProxyResponseEvent success(Object data) {
        return createResponse(200, data);
    }


    /**
     * Creates a successful response with the given data.
     *
     * @param data The data to include in the response
     * @return An API Gateway response with status code 201
     */
    public static APIGatewayProxyResponseEvent created(Object data) {
        return createResponse(201, data);
    }

    /**
     * Creates an error response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 400
     */
    public static APIGatewayProxyResponseEvent error(String message) {
        return createResponse(400, Collections.singletonMap("message", message));
    }

    /**
     * Creates an unauthorized response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 401
     */
    public static APIGatewayProxyResponseEvent unauthorized(String message) {
        return createResponse(401, Collections.singletonMap("message", message));
    }

    /**
     * Creates an Forbidden response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 401
     */
    public static APIGatewayProxyResponseEvent forbidden(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        return createResponse(403, errorResponse);
    }
    public static APIGatewayProxyResponseEvent methodNotAllowed(String message) {
        return createResponse(405, Collections.singletonMap("message", message));
    }

    /**
     * Creates a not found response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 404
     */
    public static APIGatewayProxyResponseEvent notFound(String message) {
        return createResponse(404, Collections.singletonMap("message", message));
    }
    /**
     * Creates a conflict response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 409
     */
    public static APIGatewayProxyResponseEvent conflict(String message) {
        return createResponse(409, Collections.singletonMap("message", message));
    }
    public static APIGatewayProxyResponseEvent payload(String message) {
        return createResponse(413, Collections.singletonMap("message", message));
    }

    /**
     * Creates an unauthorized response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 401
     */
    public static APIGatewayProxyResponseEvent unprocessable(String message) {
        return createResponse(422, Collections.singletonMap("message", message));
    }

    /**
     * Creates a server error response with the given message.
     *
     * @param message The error message
     * @return An API Gateway response with status code 500
     */
    public static APIGatewayProxyResponseEvent serverError(String message) {
        return createResponse(500, Collections.singletonMap("message", message));
    }

    private static APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(new HashMap<>(CORS_HEADERS));

        try {
            response.setBody(OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing response body", e);
            response.setBody("{\"message\":\"Error processing response\"}");
        }

        return response;
    }

    private static Map<String, String> createCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        return Collections.unmodifiableMap(headers);
    }
}