package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.ConflictException;

import com.restaurantapp.Exception.UnprocessableException;
import com.restaurantapp.Model.BookingRequest;
import com.restaurantapp.Model.BookingResponse;
import com.restaurantapp.Model.BookingUpdateRequest;
import com.restaurantapp.Service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class BookingController {
    private static final Logger LOG = LoggerFactory.getLogger(BookingController.class);
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @Inject
    public BookingController(BookingService bookingService, ObjectMapper objectMapper) {
        this.bookingService = bookingService;
        this.objectMapper = objectMapper;
    }

    public APIGatewayProxyResponseEvent createBooking(String requestBody, Map<String, Object> claims) {
        try {
            LOG.info("Processing booking request");

            // Extract customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isEmpty()) {
                LOG.warn("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated properly");
            }

            // Parse request body
            BookingRequest request = objectMapper.readValue(requestBody, BookingRequest.class);

            // Create booking
            BookingResponse response = bookingService.createBooking(request, customerEmail);

            return ApiResponse.created(response);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        }catch (ConflictException e) {
            LOG.warn("Booking conflict: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());  // conflict response idhar hai

        } catch (UnprocessableException e) {
            // Let this bubble up untouched!
            LOG.warn("Unavaliability during booking: {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage()); // No waiter response idhar hai
        } catch (Exception e) {
            LOG.error("Error processing booking request", e);
            return ApiResponse.serverError("Error processing booking: " + e.getMessage());
        }
    }



    //Update Booking
    public APIGatewayProxyResponseEvent updateBooking(String reservationId, String requestBody, Map<String, Object> claims) {
        try {
            LOG.info("Processing booking update request for reservation: {}", reservationId);

            // Extract customer email from claims
            String customerEmail = (String) claims.get("email");
            if (customerEmail == null || customerEmail.isEmpty()) {
                LOG.warn("Customer email not found in claims");
                return ApiResponse.unauthorized("User not authenticated properly");
            }

            // Parse request body
            BookingUpdateRequest request = objectMapper.readValue(requestBody, BookingUpdateRequest.class);

            // Update booking
            BookingResponse response = bookingService.updateBooking(reservationId, request, customerEmail);

            return ApiResponse.success(response);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid booking update request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (ConflictException e) {
            LOG.warn("Booking conflict: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        } catch (UnprocessableException e) {
            LOG.warn("Unavailability during booking update: {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing booking update request", e);
            return ApiResponse.serverError("Error updating booking: " + e.getMessage());
        }
    }
}