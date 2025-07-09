package com.restaurantapp.Service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantapp.Controller.ApiResponse;
import com.restaurantapp.Exception.UnauthorizedException;
import com.restaurantapp.Model.*;
import com.restaurantapp.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for user management operations.
 */
@Singleton
public class UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final CognitoService cognitoService;

    @Inject
    public UserService(UserRepository userRepository, CognitoService cognitoService) {
        this.userRepository = userRepository;
        this.cognitoService = cognitoService;
    }

    /**
     * Registers a new user.
     *
     * @param user The user to register
     * @return Result message
     */
    public String signUp(User user) {
        // Validate input
        if(!isValid(user).isEmpty())
            return isValid(user);
        try {
            LOG.info("Attempting to register user: {}", user.getEmail());

            // Check if email already exists
            if (userRepository.emailExists(user.getEmail())) {
                LOG.warn("Email already exists: {}", user.getEmail());
                return "Email already exists";
            }

            // Sign up user in Cognito
            cognitoService.signUpUser(user.getEmail(), user.getPassword());
            LOG.info("User registered in Cognito: {}", user.getEmail());

            // Create user in DynamoDB (without password)
            userRepository.createUser(user);
            LOG.info("User created in database: {}", user.getEmail());

            return "User created successfully";
        } catch (Exception e) {
            LOG.error("Error during user registration", e);
            return "Error during signup: " + e.getMessage();
        }
    }
    public String isValid(User user){
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            return "Email is required";
        }
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            return "First name is required";
        }
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            return "Last name is required";
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return "Password is required";
        }
        if (user.getFirstName().length()>50) {
            return "First name exceeds maximum length of 50 characters";
        }
        if (user.getLastName().length()>50) {
            return "Last name exceeds maximum length of 50 characters";
        }
        if (!user.getFirstName().matches("[a-zA-Z\\s\\-']+")) {
            return "First name contains invalid characters";
        }
        if (!user.getLastName().matches("[a-zA-Z\\s\\-']+")) {
            return "Last name contains invalid characters";
        }

        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return "Invalid email format";
        }
        // Password validations
        String password = user.getPassword();
        // Length validation (8-16 characters)
        if ((password.length() < 8 || password.length() > 16) ||(!password.matches(".*[A-Z].*"))||(!password.matches(".*[a-z].*"))||(!password.matches(".*\\d.*"))||(!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))){
            return "Password does not meet strength requirements";
        }
        else{
            return "";
        }
    }
    /**
     * Creates a new waiter account.
     *
     * @param waiter The waiter to create
     * @return Result message
     */
    public String waiterCreation(Waiter waiter) {
        // Validate input
        if (waiter.getEmail() == null || waiter.getEmail().trim().isEmpty()) {
            return "Email is required";
        }
        if (waiter.getLocationId() == null || waiter.getLocationId().trim().isEmpty()) {
            return "Location is required";
        }

        try {
            LOG.info("Attempting to create waiter: {}", waiter.getEmail());

            // Check if email already exists
            if (userRepository.waiterExists(waiter.getEmail())) {
                LOG.warn("Waiter email already exists: {}", waiter.getEmail());
                return "Email already exists";
            }

            if (!EMAIL_PATTERN.matcher(waiter.getEmail()).matches()) {
                return "Email format is incorrect";
            }

            // Create waiter in DynamoDB
            userRepository.createWaiter(waiter);
            LOG.info("Waiter created in database: {}", waiter.getEmail());

            return "Waiter created successfully";
        } catch (Exception e) {
            LOG.error("Error during waiter creation", e);
            return "Error during waiter creation: " + e.getMessage();
        }
    }

    /**
     * Authenticates a user and returns tokens.
     *
     * @param request The login request
     * @return SignInResponse with tokens and user info
     */
    public SignInResponse login(SignInRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email format is incorrect");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        try {
            LOG.info("Attempting to authenticate user: {}", email);

            // Try authenticating with Cognito
            Map<String, String> tokens = cognitoService.loginUser(email, password);

            // If tokens are returned, fetch user
            User user = userRepository.getUserByEmail(email);
            if (user == null) {
                LOG.warn("User authenticated but not found in DB: {}", email);
                throw new UnauthorizedException("Invalid credentials");
            }

            // Create response
            SignInResponse response = new SignInResponse();
            response.setAccessToken(tokens.get("accessToken"));
            response.setIdToken(tokens.get("idToken"));
            response.setRefreshToken(tokens.get("refreshToken"));
            response.setUsername(user.getFirstName() + " " + user.getLastName());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setRole(user.getRole());

            LOG.info("Login successful for user: {}", request.getEmail());
            return response;
        } catch (UnauthorizedException e) {
            LOG.warn("Invalid input or login attempt: {}", e.getMessage());
            throw e; // This will be caught and mapped to 400 or 401
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid input or login attempt: {}", e.getMessage());
            throw e; // This will be caught and mapped to 400 or 401
        } catch (Exception e) {
            LOG.error("Login failed for email {}: {}", email, e.getMessage());
            throw new IllegalArgumentException("Invalid credentials");
        }
    }


    /**
     * Gets a user by email.
     *
     * @param email The user's email
     * @return The user DTO
     */
    public UserDto getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        try {
            LOG.info("Getting user by email: {}", email);

            // Get user from repository
            User user = userRepository.getUserByEmail(email);
            if (user == null) {
                LOG.warn("User not found: {}", email);
                throw new RuntimeException("User not found");
            }

            // Convert to DTO
            UserDto userDto = new UserDto(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole()
            );

            return userDto;
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request for user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error getting user", e);
            throw new RuntimeException("Error getting user: " + e.getMessage(), e);
        }
    }

    public APIGatewayProxyResponseEvent deleteWaiter(String requestBody) {
        String email = extractJsonValue("email", requestBody);
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        return ApiResponse.success("waiter deleted");
    }


    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"(.*?)\"";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}