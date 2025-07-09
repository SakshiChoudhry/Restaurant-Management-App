package com.restaurantapp.Service;

import com.restaurantapp.Exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for AWS Cognito operations.
 */
@Singleton
public class CognitoService
{
    private static final Logger LOG = LoggerFactory.getLogger(CognitoService.class);

    private final CognitoIdentityProviderClient cognitoClient;
    private final String userPoolId;
    private final String clientId;

    @Inject
    public CognitoService() {
        // Initialize Cognito client
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();

        // Find user pool ID by name
        String poolName = System.getenv("cognito_pool");
        this.userPoolId = findUserPoolIdByName(poolName);
        LOG.info("User Pool ID: {}", this.userPoolId);

        if (this.userPoolId == null || this.userPoolId.isEmpty()) {
            throw new RuntimeException("Could not find user pool with name: " + poolName);
        }

        // Find client ID by name
        String clientName = "restaurant-app-client";
        this.clientId = findClientIdByName(this.userPoolId, clientName);
        LOG.info("Client ID: {}", this.clientId);

        if (this.clientId == null || this.clientId.isEmpty()) {
            throw new RuntimeException("Could not find client with name: " + clientName + " in user pool: " + this.userPoolId);
        }
    }

    /**
     * Finds a user pool ID by name.
     *
     * @param poolName The name of the user pool
     * @return The user pool ID or null if not found
     */
    private String findUserPoolIdByName(String poolName) {
        try {
            LOG.info("Looking for user pool with name: {}", poolName);

            ListUserPoolsRequest listPoolsRequest = ListUserPoolsRequest.builder()
                    .maxResults(60)
                    .build();

            ListUserPoolsResponse listPoolsResponse = cognitoClient.listUserPools(listPoolsRequest);

            for (UserPoolDescriptionType pool : listPoolsResponse.userPools()) {
                LOG.debug("Found user pool: {} with ID: {}", pool.name(), pool.id());

                if (pool.name().equals(poolName)) {
                    return pool.id();
                }
            }

            LOG.warn("User pool not found: {}", poolName);
            return null;
        } catch (Exception e) {
            LOG.error("Error finding user pool", e);
            return null;
        }
    }

    /**
     * Finds a client ID by name in a user pool.
     *
     * @param userPoolId The user pool ID
     * @param clientName The client name
     * @return The client ID or null if not found
     */
    private String findClientIdByName(String userPoolId, String clientName) {
        try {
            LOG.info("Looking for client with name: {} in user pool: {}", clientName, userPoolId);

            ListUserPoolClientsRequest listClientsRequest = ListUserPoolClientsRequest.builder()
                    .userPoolId(userPoolId)
                    .maxResults(60)
                    .build();

            ListUserPoolClientsResponse listClientsResponse = cognitoClient.listUserPoolClients(listClientsRequest);

            for (UserPoolClientDescription client : listClientsResponse.userPoolClients()) {
                LOG.debug("Found client: {} with ID: {}", client.clientName(), client.clientId());

                if (client.clientName().equals(clientName)) {
                    return client.clientId();
                }
            }

            LOG.warn("Client not found: {} in user pool: {}", clientName, userPoolId);
            return null;
        } catch (Exception e) {
            LOG.error("Error finding client", e);
            return null;
        }
    }

    /**
     * Registers a new user in Cognito.
     *
     * @param email The user's email
     * @param password The user's password
     */
    public void signUpUser(String email, String password) {
        try {
            LOG.info("Signing up user in Cognito: {}", email);

            // Create attribute map
            AttributeType emailAttribute = AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build();

            // Create signup request
            SignUpRequest signUpRequest = SignUpRequest.builder()
                    .clientId(clientId)
                    .username(email)
                    .password(password)
                    .userAttributes(emailAttribute)
                    .build();

            // Execute signup
            SignUpResponse response = cognitoClient.signUp(signUpRequest);
            LOG.info("User signed up in Cognito with sub: {}", response.userSub());

            // Auto-confirm the user
            AdminConfirmSignUpRequest confirmRequest = AdminConfirmSignUpRequest.builder()
                    .userPoolId(userPoolId)
                    .username(email)
                    .build();

            cognitoClient.adminConfirmSignUp(confirmRequest);
            LOG.info("User confirmed in Cognito: {}", email);

        } catch (UsernameExistsException e) {
            LOG.warn("User already exists in Cognito: {}", email);
            throw new RuntimeException("User already exists in Cognito", e);
        } catch (Exception e) {
            LOG.error("Error signing up user in Cognito", e);
            throw new RuntimeException("Error signing up user in Cognito", e);
        }
    }

    /**
     * Authenticates a user and returns tokens.
     *
     * @param email The user's email
     * @param password The user's password
     * @return Map containing the tokens
     */
    public Map<String, String> loginUser(String email, String password) {
        try {
            LOG.info("Logging in user: {}", email);

            // Create auth parameters
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", email);
            authParams.put("PASSWORD", password);

            // Create auth request
            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(userPoolId)
                    .clientId(clientId)
                    .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .authParameters(authParams)
                    .build();

            // Execute auth request
            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

            // Extract tokens
            AuthenticationResultType authResult = authResponse.authenticationResult();

            Map<String, String> tokens = new HashMap<>();
            tokens.put("idToken", authResult.idToken());
            tokens.put("accessToken", authResult.accessToken());
            tokens.put("refreshToken", authResult.refreshToken());

            LOG.info("User logged in successfully: {}", email);
            return tokens;
        } catch (NotAuthorizedException e) {
            LOG.warn("Invalid credentials for user: {}", email);
            throw new UnauthorizedException("Invalid credentials");
        } catch (Exception e) {
            LOG.error("Error logging in user", e);
            throw new UnauthorizedException("Invalid Credentials");
        }
    }

    /**
     * Validates an access token and returns the claims if valid.
     *
     * @param accessToken The access token to validate
     * @return Map containing the token claims if valid, null otherwise
     */
    public Map<String, Object> validateAccessToken(String accessToken) {
        try {
            // Use AWS SDK to validate the token
            GetUserRequest getUserRequest = GetUserRequest.builder()
                    .accessToken(accessToken)
                    .build();

            GetUserResponse getUserResponse = cognitoClient.getUser(getUserRequest);

            // If we get here, the token is valid
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", getUserResponse.username());

            // Add user attributes
            getUserResponse.userAttributes().forEach(attribute -> {
                claims.put(attribute.name(), attribute.value());
            });

            return claims;
        } catch (NotAuthorizedException e) {
            LOG.warn("Invalid or expired token");
            return null;
        } catch (Exception e) {
            LOG.error("Error validating token", e);
            return null;
        }
    }

    /**
     * Refreshes tokens using a refresh token.
     *
     * @param refreshToken The refresh token
     * @return Map containing the new tokens
     */
    public Map<String, String> refreshTokens(String refreshToken) {
        try {
            LOG.info("Refreshing tokens");

            // Create auth parameters
            Map<String, String> authParams = new HashMap<>();
            authParams.put("REFRESH_TOKEN", refreshToken);

            // Create auth request
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .clientId(clientId)
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .authParameters(authParams)
                    .build();

            // Execute request
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);

            // Extract tokens
            AuthenticationResultType authResult = authResponse.authenticationResult();
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", authResult.accessToken());
            tokens.put("idToken", authResult.idToken());

            // Note: Refresh token is not returned when using REFRESH_TOKEN_AUTH flow
            // unless the refresh token has expired
            if (authResult.refreshToken() != null && !authResult.refreshToken().isEmpty()) {
                tokens.put("refreshToken", authResult.refreshToken());
            }

            LOG.info("Tokens refreshed successfully");
            return tokens;
        } catch (NotAuthorizedException e) {
            LOG.warn("Invalid refresh token");
            return null;
        } catch (Exception e) {
            LOG.error("Error refreshing tokens", e);
            return null;
        }
    }
}