package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantapp.Model.AvailableTable;
import com.restaurantapp.Model.Location;
import com.restaurantapp.Service.TableService;
import com.restaurantapp.Repository.RestaurantLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class TableController {
    private static final Logger LOG = LoggerFactory.getLogger(TableController.class);
    private final TableService tableService;
    private final RestaurantLocationRepository locationRepository;

    public TableController() {
        this.tableService = new TableService();
        this.locationRepository = new RestaurantLocationRepository();
    }

    public APIGatewayProxyResponseEvent handleTableReservations(APIGatewayProxyRequestEvent request) {
        try {
            LOG.info("Processing table reservations request");

            Map<String, String> queryParams = request.getQueryStringParameters();
            if (queryParams == null) {
                queryParams = new HashMap<>();
            }

            // Validate that only allowed parameters are present
            try {
                validateQueryParameters(queryParams);
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid query parameter: {}", e.getMessage());
                return ApiResponse.error(e.getMessage());
            }

            // Extract and validate locationId
            String locationId = getLocationId(queryParams);
            if (locationId != null && !locationId.isEmpty()) {
                // First validate format
                if (!locationId.matches("^\\d+$")) {
                    LOG.warn("Invalid locationId format: {}", locationId);
                    return ApiResponse.error("Invalid locationId: must be a number");
                }

                // Then validate existence
                if (!isLocationExists(locationId)) {
                    LOG.warn("Location not found: {}", locationId);
                    return ApiResponse.error("Invalid locationId: Location not found");
                }
            }

            // Validate date if provided
            String dateStr = queryParams != null ? queryParams.get("date") : null;
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    LocalDate today = LocalDate.now();

                    // Check if date is in the past
                    if (date.isBefore(today)) {
                        LOG.warn("Past date requested: {}", dateStr);
                        return ApiResponse.error("Invalid date: Cannot book tables for past dates");
                    }

                    // Check if date is more than 3 months in the future
                    LocalDate maxDate = today.plusMonths(3);
                    if (date.isAfter(maxDate)) {
                        LOG.warn("Date too far in future: {}", dateStr);
                        return ApiResponse.error("Invalid date: Bookings are only available up to 3 months in advance");
                    }
                } catch (DateTimeParseException e) {
                    LOG.warn("Invalid date format: {}", dateStr);
                    return ApiResponse.error("Invalid date format. Please use YYYY-MM-DD");
                }
            }

            try {
                List<AvailableTable> availableTables = getAvailableTables(queryParams);
                LOG.info("Found {} available tables", availableTables.size());
                return ApiResponse.success(availableTables);
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request parameter value: {}", e.getMessage());
                return ApiResponse.error(e.getMessage());
            }

        } catch (Exception e) {
            LOG.error("Error processing table reservations request", e);
            return ApiResponse.serverError("An unexpected error occurred while processing your request");
        }
    }

    /**
     * Validates that only allowed query parameters are present
     * @param queryParams The query parameters to validate
     * @throws IllegalArgumentException if an invalid parameter is found
     */
    private void validateQueryParameters(Map<String, String> queryParams) {
        // Define the list of valid parameter names
        Set<String> validParams = new HashSet<>(Arrays.asList(
                "locationId", "location_id", "date", "time", "timeslot", "guests"
        ));

        // Check for invalid parameters
        if (queryParams != null) {
            for (String param : queryParams.keySet()) {
                if (!validParams.contains(param)) {
                    throw new IllegalArgumentException("Invalid query parameter: " + param);
                }
            }
        }
    }

    /**
     * Extract locationId from query parameters
     */
    private String getLocationId(Map<String, String> queryParams) {
        if (queryParams == null) return null;

        if (queryParams.containsKey("locationId")) {
            return queryParams.get("locationId");
        } else if (queryParams.containsKey("location_id")) {
            return queryParams.get("location_id");
        }

        return null;
    }

    /**
     * Check if the location ID exists in the database
     */
    private boolean isLocationExists(String locationId) {
        try {
            List<Location> locations = locationRepository.findAll();
            return locations.stream()
                    .anyMatch(location -> location.getLocationId().equals(locationId));
        } catch (Exception e) {
            LOG.error("Error checking location existence: {}", e.getMessage(), e);
            // If we can't check, assume it doesn't exist to be safe
            return false;
        }
    }

    public List<AvailableTable> getAvailableTables(Map<String, String> queryParams) {
        // Handle both locationId and location_id
        String locationId = getLocationId(queryParams);

        String date = queryParams != null ? queryParams.get("date") : null;

        // Handle both time and timeslot
        String time = null;
        if (queryParams != null) {
            if (queryParams.containsKey("time")) {
                time = queryParams.get("time");
            } else if (queryParams.containsKey("timeslot")) {
                time = queryParams.get("timeslot");
            }
        }

        // Validate and parse guests
        Integer guests = null;
        if (queryParams != null && queryParams.containsKey("guests")) {
            String guestsStr = queryParams.get("guests");
            if (guestsStr != null && !guestsStr.isEmpty()) {
                try {
                    guests = Integer.parseInt(guestsStr);
                    if (guests < 1) {
                        throw new IllegalArgumentException("Guests must be at least 1");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid guests parameter: must be a number");
                }
            }
        }

        return tableService.findAvailableTables(locationId, date, time, guests);
    }
}