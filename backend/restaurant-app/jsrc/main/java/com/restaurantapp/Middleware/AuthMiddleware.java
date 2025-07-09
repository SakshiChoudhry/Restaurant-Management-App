package com.restaurantapp.Middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.restaurantapp.Service.CognitoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Middleware for handling authentication and authorization.
 */
@Singleton
public class AuthMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(AuthMiddleware.class);
    private final CognitoService cognitoService;

    @Inject
    public AuthMiddleware(CognitoService cognitoService) {
        this.cognitoService = cognitoService;
    }

    /**
     * Validates the user's authentication using cookies.
     *
     * @param request The API Gateway request
     * @return Map containing the user claims if valid, null otherwise
     */
    public Map<String, Object> validateToken(APIGatewayProxyRequestEvent request) {
        try {
            // First try to get token from Authorization header (for localStorage approach)
            String accessToken = null;
            String refreshToken = null;

            Map<String, String> headers = request.getHeaders();
            if (headers != null && headers.containsKey("Authorization")) {
                String authHeader = headers.get("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    accessToken = authHeader.substring(7); // Remove "Bearer " prefix
                }
            }

            if (accessToken == null || accessToken.isEmpty()) {
            Map<String, String> cookies = extractCookies(request);

            accessToken = cookies.get("AccessToken");
            refreshToken = cookies.get("RefreshToken");


                LOG.warn("No access token in cookies");
                return null;
            }

            // Try to validate the access token
            Map<String, Object> claims = cognitoService.validateAccessToken(accessToken);

            // If access token is valid, return claims
            if (claims != null) {
                return claims;
            }

            // If access token is invalid but we have a refresh token, try to refresh
            if (refreshToken != null && !refreshToken.isEmpty()) {
                LOG.info("Access token invalid, trying to refresh");

                Map<String, String> newTokens = cognitoService.refreshTokens(refreshToken);

                if (newTokens != null && newTokens.containsKey("accessToken")) {
                    claims = cognitoService.validateAccessToken(newTokens.get("accessToken"));

                    if (claims != null) {
                        // Add the new tokens to the response headers as cookies
                        Map<String, List<String>> responseHeaders = new HashMap<>();

                        // Access token cookie - short lived (1 hour)
                        String accessTokenCookie = String.format(
                                "AccessToken=%s; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=3600",
                                newTokens.get("accessToken")
                        );

                        // If we got a new refresh token
                        if (newTokens.containsKey("refreshToken")) {
                            String refreshTokenCookie = String.format(
                                    "RefreshToken=%s; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=2592000",
                                    newTokens.get("refreshToken")
                            );
                            responseHeaders.put("Set-Cookie", List.of(accessTokenCookie, refreshTokenCookie));
                        } else {
                            responseHeaders.put("Set-Cookie", List.of(accessTokenCookie));
                        }

                        // Store the headers in the request context for later use
                        request.getRequestContext().setAuthorizer(
                                Map.of("responseHeaders", responseHeaders)
                        );

                        return claims;
                    }
                }
            }

            LOG.warn("Authentication failed");
            return null;
        } catch (Exception e) {
            LOG.error("Error in auth middleware", e);
            return null;
        }
    }

    /**
     * Extracts cookies from the request.
     *
     * @param request The API Gateway request
     * @return Map of cookie names to values
     */
    private Map<String, String> extractCookies(APIGatewayProxyRequestEvent request) {
        Map<String, String> cookies = new HashMap<>();

        Map<String, String> headers = request.getHeaders();
        if (headers != null && headers.containsKey("Cookie")) {
            String cookieHeader = headers.get("Cookie");
            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                String[] cookiePairs = cookieHeader.split("; ");
                for (String cookiePair : cookiePairs) {
                    String[] cookieParts = cookiePair.split("=", 2);
                    if (cookieParts.length == 2) {
                        cookies.put(cookieParts[0], cookieParts[1]);
                    }
                }
            }
        }

        return cookies;
    }
}