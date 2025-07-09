package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.ConflictException;
import com.restaurantapp.Model.ReservationResponse;
import com.restaurantapp.Service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class ReservationController
{
    private static final Logger LOG = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;
    private final ObjectMapper objectMapper;

    @Inject
    public ReservationController(ReservationService reservationService, ObjectMapper objectMapper)
    {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets all reservations for the authenticated user.
     *
     * @param claims The user claims from the authentication token
     * @return API Gateway response with reservation data
     */
    public APIGatewayProxyResponseEvent getUserReservations(Map<String, Object> claims)
    {
        try {
            String email = (String) claims.get("email");
            if (email == null || email.isEmpty()) {
                LOG.warn("Email not found in token claims");
                return ApiResponse.unauthorized("User email not found in token");
            }

            LOG.info("Getting reservations for authenticated user: {}", email);

            List<ReservationResponse> reservations = reservationService.getReservationsForUser(email);
            LOG.info("Found {} reservations for user: {}", reservations.size(), email);
            return ApiResponse.success(reservations);
        }
        catch (ConflictException e) {
            LOG.warn("Time constraint violation for cancellation: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        }
        catch (Exception e) {
            LOG.error("Error getting user reservations", e);
            return ApiResponse.serverError("Error retrieving reservations: " + e.getMessage());
        }
    }
}
