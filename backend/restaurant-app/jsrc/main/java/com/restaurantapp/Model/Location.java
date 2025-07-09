package com.restaurantapp.Model;

public class Location
{
    private String locationId;
    private String locationAddress;
    private String averageOccupancy;
    private String description;
    private String imageURL;
    private String rating;
    private String totalCapacity;

    public Location() {
    }

    public Location(String locationId, String address, String description, String totalCapacity, String averageOccupancy, String imageUrl, String rating) {

        this.locationId = locationId;
        this.locationAddress = address;
        this.averageOccupancy = averageOccupancy;
        this.description = description;
        this.imageURL = imageUrl;
        this.rating = rating;
        this.totalCapacity = totalCapacity;

    }

    public String getLocationId()
    {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(String locationAddress) {
        this.locationAddress = locationAddress;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getImageURL()
    {
        return imageURL;
    }

    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    public String getAverageOccupancy() {
        return averageOccupancy;
    }

    public void setAverageOccupancy(String averageOccupancy) {
        this.averageOccupancy = averageOccupancy;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(String totalCapacity) {
        this.totalCapacity = totalCapacity;
    }
}
