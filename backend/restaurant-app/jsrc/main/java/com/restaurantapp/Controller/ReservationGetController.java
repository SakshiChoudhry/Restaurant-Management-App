package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.ConflictException;
import com.restaurantapp.Model.ReservationGetResponse;
import com.restaurantapp.Service.ReservationGetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class ReservationGetController
{
    private static final Logger LOG = LoggerFactory.getLogger(ReservationGetController.class);

    private final ReservationGetService reservationService;
    private final ObjectMapper objectMapper;

    @Inject
    public ReservationGetController(ReservationGetService reservationService, ObjectMapper objectMapper)
    {
        this.reservationService = reservationService;
        this.objectMapper = objectMapper;
    }

    public APIGatewayProxyResponseEvent getUserReservations(Map<String, Object> claims)
    {
        try {

            String email = (String) claims.get("email");
            LOG.info("email is {}",email);
            if (email == null || email.isEmpty()) {
                LOG.warn("Email not found in token claims");
                return ApiResponse.unauthorized("User email not found in token");
            }

            LOG.info("Getting reservations for authenticated user: {}", email);

            List<ReservationGetResponse> reservations = reservationService.getReservationsForUser(email);
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
