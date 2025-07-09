package com.restaurantapp.Model;

public class Waiter {
    private String email;
    private String locationId;  // Add this field

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }



    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public Waiter(String email, String locationId) {
        this.email = email;
        this.locationId = locationId;
    }

    public Waiter() {
    }
}