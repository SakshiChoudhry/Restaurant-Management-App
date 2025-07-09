package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantapp.Model.UserDto;
import com.restaurantapp.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Controller for user profile operations.
 */
@Singleton
public class ProfileController {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileController.class);
    private final UserService userService;

    @Inject
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gets the profile of the authenticated user.
     *
     * @param claims The authenticated user claims
     * @return API Gateway response with user profile
     */
    public APIGatewayProxyResponseEvent getProfile(Map<String, Object> claims) {
        try {
            String email = (String) claims.get("email");
            LOG.info("Getting profile for user: {}", email);

            UserDto user = userService.getUserByEmail(email);
            return ApiResponse.success(user);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request for profile: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error getting profile", e);
            return ApiResponse.serverError("Error getting profile: " + e.getMessage());
        }
    }
}