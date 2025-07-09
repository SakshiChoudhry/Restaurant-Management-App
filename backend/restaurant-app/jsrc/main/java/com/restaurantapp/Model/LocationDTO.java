package com.restaurantapp.Model;

/**
 * Data Transfer Object (DTO) for locations.
 * Ensures that only `locationId` and `address` are exposed in the API response.
 */
public class LocationDTO {
    private final String id;
    private final String address;

    public LocationDTO(String locationId, String address) {
        this.id = locationId;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }
}
