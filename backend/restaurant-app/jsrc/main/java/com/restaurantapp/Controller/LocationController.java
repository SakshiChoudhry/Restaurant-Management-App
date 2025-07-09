package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Model.Location;
import com.restaurantapp.Model.LocationDTO;
import com.restaurantapp.Model.SpecialityDishes;
import com.restaurantapp.Service.LocationService;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LocationController {
    private final ObjectMapper objectMapper;
    private final LocationService locationService;
    private static final Logger LOG = Logger.getLogger(LocationController.class.getName());

    public LocationController(LocationService locationService) {
        this.objectMapper=new ObjectMapper();
        this.locationService = locationService;
    }

    public APIGatewayProxyResponseEvent getAllLocations() {
        LOG.info("getAllLocations");
        try {
            List<Location> locationList=locationService.getAllLocations();
            if(locationList.isEmpty() || locationList.size()==0){
                return ApiResponse.error("Locations are not available.");
            }
            else{
                return ApiResponse.success(Map.of("message",locationList));
            }
        } catch (Exception e) {
            return ApiResponse.serverError("Error during fetching locations: " + e.getMessage());
        }
    }

    /**
     * Fetches all restaurant locations.
     *
     * @return API response with location details.
     */
    public APIGatewayProxyResponseEvent getLocations() {
        try {
            LOG.info("Fetching all restaurant locations");
            List<LocationDTO> locations = locationService.getAllLocationsBySakshi();

//            String responseBody = objectMapper.writeValueAsString(Map.of("locations", locations));
            return ApiResponse.success(locations);
        } catch (Exception e) {
            LOG.info("Error fetching locations");
            return ApiResponse.serverError("Error fetching locations: " + e.getMessage());
        }
    }


    public APIGatewayProxyResponseEvent getSpecialityDishes(String locationId){
        try{
            boolean locationExists = locationService.locationExists(locationId);

            if (!locationExists) {
                // Return a special string to indicate location not found
                return ApiResponse.notFound("Invalid location ID");
            }
            List<SpecialityDishes> specialityDishes=locationService.getSpecialityDishes(locationId);
            return ApiResponse.success(specialityDishes);
        } catch (Exception e){
            return ApiResponse.serverError("Failed to fetch speciality dishes of a locations due to: "+e.getMessage());
        }
    }
}
