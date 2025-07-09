package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.*;
import com.restaurantapp.Model.CustomerFeedback;
import com.restaurantapp.Model.CustomerFeedbackRequest;
import com.restaurantapp.Model.CustomerFeedbackResponse;
import com.restaurantapp.Service.CustomerFeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class CustomerFeedbackController {
    private static final Logger LOG = LoggerFactory.getLogger(CustomerFeedbackController.class);

    private CustomerFeedbackService customerFeedbackService;
    private ObjectMapper objectMapper;

    @Inject
    public CustomerFeedbackController(CustomerFeedbackService customerFeedbackService, ObjectMapper objectMapper) {
        this.customerFeedbackService = customerFeedbackService;
        this.objectMapper = objectMapper;
    }

    public APIGatewayProxyResponseEvent createFeedback(String requestBody, Map<String, Object> claims) {
        try {
            LOG.info("Processing booking request");

            // Extract customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isEmpty()) {
                LOG.warn("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated properly");
            }

            // Parse request body
            CustomerFeedbackRequest request = objectMapper.readValue(requestBody, CustomerFeedbackRequest.class);

            // Create feedback
            String feedback = customerFeedbackService.createFeedback(request, customerEmail);

            return ApiResponse.created(Map.of("message", feedback));

            //catches
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid Reservation request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (PayloadTooLarge e) {
            LOG.warn("Invalid Reservation request: {}", e.getMessage());
            return ApiResponse.payload(e.getMessage());
        } catch (ConflictException e) {
            LOG.warn("Reservation conflict: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        } catch (UnprocessableException e) {
            LOG.warn("Unprocessable Request: {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage());
        } catch (BadRequestException e) {
            LOG.warn("Unavailability for booking : {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (ForbiddenException e) {
            LOG.warn("Unavailability for booking : {}", e.getMessage());
            return ApiResponse.forbidden(e.getMessage());
        }catch (NotFoundException e) {
            LOG.warn("Unavailability for booking : {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing Reservation request", e);
            return ApiResponse.serverError("Error processing Reservation baba: " + e.getMessage());
        }

    }


    //UPDATE
    public APIGatewayProxyResponseEvent updateFeedback(String requestBody, Map<String, Object> claims) {
        try {
            LOG.info("Processing feedback update request for feedbackId: ");

            // Extract customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isEmpty()) {
                LOG.warn("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated properly");
            }

            // Parse request body
            CustomerFeedbackRequest request = objectMapper.readValue(requestBody, CustomerFeedbackRequest.class);

            // Update feedback
            String result = customerFeedbackService.updateFeedback(request, customerEmail);

            return ApiResponse.success(Map.of("message", result));

        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid feedback update request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (PayloadTooLarge e) {
            LOG.warn("Invalid feedback update request: {}", e.getMessage());
            return ApiResponse.payload(e.getMessage());
        } catch (ConflictException e) {
            LOG.warn("Feedback update conflict: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        } catch (UnprocessableException e) {
            LOG.warn("Unprocessable Request: {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage());
        } catch (BadRequestException e) {
            LOG.warn("Bad request for feedback update: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (NotFoundException e) {
            LOG.warn("Feedback not found: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (UnauthorizedException e) {
            LOG.warn("Unauthorized feedback update: {}", e.getMessage());
            return ApiResponse.unauthorized(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing feedback update request", e);
            return ApiResponse.serverError("Error processing feedback update: " + e.getMessage());
        }
    }

    public APIGatewayProxyResponseEvent getFeedback(String reservationId, Map<String, Object> claims) {
        try {
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isEmpty()) {
                LOG.warn("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated properly");
            }

            List<CustomerFeedbackResponse> listFeedback = customerFeedbackService.getAllFeedbacksForReservation(reservationId,customerEmail);
            LOG.info("List of feedback: {}", listFeedback);

            return ApiResponse.success(listFeedback);
        }  catch (IllegalArgumentException e) {
            LOG.warn("Invalid feedback update request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (PayloadTooLarge e) {
            LOG.warn("Invalid feedback update request: {}", e.getMessage());
            return ApiResponse.payload(e.getMessage());
        } catch (ConflictException e) {
            LOG.warn("Feedback update conflict: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        } catch (UnprocessableException e) {
            LOG.warn("Unprocessable Request: {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage());
        } catch (BadRequestException e) {
            LOG.warn("Bad request for feedback update: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (NotFoundException e) {
            LOG.warn("Feedback not found: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (UnauthorizedException e) {
            LOG.warn("Unauthorized feedback update: {}", e.getMessage());
            return ApiResponse.unauthorized(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing feedback update request", e);
            return ApiResponse.serverError("Error processing feedback update: " + e.getMessage());
        }
    }
}
