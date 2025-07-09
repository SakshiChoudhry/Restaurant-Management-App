package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantapp.Exception.UnauthorizedException;
import com.restaurantapp.Model.AnonymousFeedbackAuthResponse;
import com.restaurantapp.Service.AnonymousFeedbackService;
import com.restaurantapp.Service.FeedbackService;
import com.restaurantapp.Controller.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AnonymousFeedbackController {
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackController.class);
    private final AnonymousFeedbackService feedbackService;

    @Inject
    public AnonymousFeedbackController(AnonymousFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Authenticate a feedback request using reservation ID and secret code
     * @param reservationId The reservation ID
     * @param secretCode The secret code
     * @return APIGatewayProxyResponseEvent with feedback auth response
     */
    public APIGatewayProxyResponseEvent authenticateFeedback(String reservationId, String secretCode) {
        try {
            LOG.info("Processing feedback authentication request for reservation: {}", reservationId);

            if (reservationId == null || reservationId.trim().isEmpty()) {
                LOG.warn("Missing reservation ID in feedback authentication request");
                return ApiResponse.error("Reservation ID is required");
            }

            if (secretCode == null || secretCode.trim().isEmpty()) {
                LOG.warn("Missing secret code in feedback authentication request");
                return ApiResponse.error("Secret code is required");
            }

            AnonymousFeedbackAuthResponse response = feedbackService.authenticateFeedback(reservationId, secretCode);
            LOG.info("Feedback authentication successful for reservation: {}", reservationId);

            return ApiResponse.success(response);
        } catch (UnauthorizedException e) {
            LOG.warn("Unauthorized feedback authentication: {}", e.getMessage());
            return ApiResponse.unauthorized(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing feedback authentication: {}", e.getMessage(), e);
            return ApiResponse.serverError("Error processing feedback authentication");
        }
    }
}