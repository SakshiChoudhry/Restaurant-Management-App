package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.restaurantapp.Exception.UnauthorizedException;
import com.restaurantapp.Model.*;
import com.restaurantapp.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for user management operations.
 */
@Singleton
public class UserController {
    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Inject
    public UserController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registers a new user.
     *
     * @param requestBody The user to register
     * @return Result message
     */
    public APIGatewayProxyResponseEvent signUp(String requestBody) {
        try {
            User user = objectMapper.readValue(requestBody, User.class);
            LOG.info("Processing sign-up request for: {}", user.getEmail());

            String result = userService.signUp(user);
            if(result.equals("Email already exists"))
                return ApiResponse.conflict(result);
            if (result.equals("User created successfully")) {
                return ApiResponse.created(Map.of("message", result));
            } else {
                return ApiResponse.error(result);
            }
        }  catch (UnauthorizedException e) {
            LOG.warn("Invalid login request: {}", e.getMessage());
            return ApiResponse.unauthorized(e.getMessage());
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid login request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error during sign-up", e);
            return ApiResponse.serverError("Error during sign-up: " + e.getMessage());
        }
    }

    /**
     * Creates a new waiter account.
     *
     * @param requestBody The waiter data
     * @return Result message
     */
    public APIGatewayProxyResponseEvent createWaiter(String requestBody) {
        try {
            Waiter waiter = objectMapper.readValue(requestBody, Waiter.class);
            LOG.info("Processing waiter creation request for: {}", waiter.getEmail());

            String result = userService.waiterCreation(waiter);

            if (result.equals("Waiter created successfully")) {
                return ApiResponse.created(Map.of("message", result));
            } else if (result.equals("A user with this email address already exists.")) {
                return ApiResponse.error(result);
            } else {
                return ApiResponse.error(result);
            }
        } catch (Exception e) {
            LOG.error("Error during waiter creation", e);
            return ApiResponse.serverError("Error creating waiter: " + e.getMessage());
        }
    }

    /**
     * Authenticates a user and returns tokens.
     *
     * @param requestBody The login request
     * @return API Gateway response with tokens
     */
    public APIGatewayProxyResponseEvent login(String requestBody) {
        try {
            SignInRequest request = objectMapper.readValue(requestBody, SignInRequest.class);
            LOG.info("Processing login request for: {}", request.getEmail());

            SignInResponse response = userService.login(request);

            // Create response with cookies
            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(200);

            // Set headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
            apiResponse.setHeaders(headers);

            // Set cookies
            Map<String, List<String>> multiValueHeaders = new HashMap<>();

            // Access token cookie - short lived (1 hour)
            String accessTokenCookie = String.format(
                    "AccessToken=%s; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=3600",
                    response.getAccessToken()
            );

            // Refresh token cookie - longer lived (30 days)
            String refreshTokenCookie = String.format(
                    "RefreshToken=%s; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=2592000",
                    response.getRefreshToken()
            );

            multiValueHeaders.put("Set-Cookie", Arrays.asList(accessTokenCookie, refreshTokenCookie));
            apiResponse.setMultiValueHeaders(multiValueHeaders);

            // Create DTO for response body (without sensitive tokens)
            SignInResponseDto responseDto = new SignInResponseDto(
                    response.getAccessToken(),
                    response.getUsername(),
                    response.getRole()
            );

            apiResponse.setBody(objectMapper.writeValueAsString(responseDto));
            return apiResponse;

        }  catch (UnauthorizedException e) {
            LOG.warn("Invalid login request: {}", e.getMessage());
            return ApiResponse.unauthorized(e.getMessage());
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid login request: {}", e.getMessage());
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error during login", e);
            return ApiResponse.serverError("Login failed: " + e.getMessage());
        }
    }

    public APIGatewayProxyResponseEvent updateRole(String requestBody) {
        APIGatewayProxyResponseEvent apiGatewayProxyResponseEvent = userService.deleteWaiter(requestBody);
        LOG.info("Processing update role request for: {}", requestBody);
        return ApiResponse.success(Map.of("message", requestBody));

    }
}