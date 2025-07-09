package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.ConflictException;
import com.restaurantapp.Exception.UnprocessableException;
import com.restaurantapp.Service.ReservationDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class ReservationDeletionController {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationDeletionController.class);
    private final ReservationDeletionService reservationDeletionService;
    private final ObjectMapper objectMapper;

    @Inject
    public ReservationDeletionController(ReservationDeletionService reservationDeletionService, ObjectMapper objectMapper) {
        this.reservationDeletionService = reservationDeletionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Cancel a reservation
     * @param reservationId The reservation ID
     * @param claims The authentication claims containing the user's email
     * @return API Gateway response
     */
    public APIGatewayProxyResponseEvent cancelReservation(String reservationId, Map<String, Object> claims) {
        try {
            LOG.info("Processing cancel reservation request for ID: {}", reservationId);

            // Extract user email from claims
            String userEmail = (String) claims.get("email");
            if (userEmail == null || userEmail.isEmpty()) {
                LOG.warn("User email not found in claims");
                return ApiResponse.unauthorized("User not authenticated properly");
            }

            // Cancel the reservation
            reservationDeletionService.cancelReservation(reservationId, userEmail);

            // Create success response
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("message", "Reservation cancelled successfully");

            return ApiResponse.success(responseBody);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid reservation cancellation request: {}", e.getMessage());
            return ApiResponse.notFound(e.getMessage());
        } catch (SecurityException e) {
            LOG.warn("Unauthorized reservation cancellation attempt: {}", e.getMessage());
            return ApiResponse.forbidden(e.getMessage());
        } catch (ConflictException e) {
            LOG.warn("Conflict Occurred {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        }catch (UnprocessableException e) {
            LOG.warn("Cannot Process {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing reservation cancellation request", e);
            return ApiResponse.serverError("Error cancelling reservation: " + e.getMessage());
        }
    }
}