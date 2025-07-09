package com.restaurantapp.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Model.Location;
import com.restaurantapp.Model.LocationDTO;
import com.restaurantapp.Model.SpecialityDishes;
import com.restaurantapp.Repository.LocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class LocationService {
    private final ObjectMapper objectMapper;
    private final LocationRepository locationRepository;
    private final Logger LOG = LoggerFactory.getLogger(LocationService.class);

    public LocationService(LocationRepository locationRepository) {
        this.objectMapper = new ObjectMapper();
        this.locationRepository=locationRepository;
    }
    public List<Location> getAllLocations() throws JsonProcessingException {
        List<Location> locationList=locationRepository.findAll();
        return locationList;
    }
    public boolean locationExists(String locationId) {
        return locationRepository.findLocationById(locationId).isPresent();
    }
    public List<SpecialityDishes> getSpecialityDishes(String locationId) throws JsonProcessingException {
        List<SpecialityDishes> specialityDishes=locationRepository.findSpecialityDishes(locationId);
        return specialityDishes;
    }
    public List<LocationDTO> getAllLocationsBySakshi() {
        try {
            LOG.info("Fetching all locations from database");
            return locationRepository.getAllLocationsBySakshi();
        } catch (Exception e) {
            LOG.error("Error fetching locations", e);
            throw new RuntimeException("Error fetching locations: " + e.getMessage(), e);
        }
    }

}
