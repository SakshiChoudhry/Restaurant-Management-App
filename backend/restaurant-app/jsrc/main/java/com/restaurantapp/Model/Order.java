package com.restaurantapp.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Order {
    @JsonProperty("id")
    private String id;

    @JsonProperty("customerEmail")
    private String customerEmail;

    @JsonProperty("date")
    private String date;

    @JsonProperty("dishItems")
    private List<DishItem> dishItems;

    @JsonProperty("locationId")
    private String locationId;

    @JsonProperty("reservationId")
    private String reservationId;

    @JsonProperty("state")
    private String state;

    @JsonProperty("timeSlot")
    private String timeSlot;

    @JsonProperty("address")
    private String address;

    // Constructors
    public Order() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public List<DishItem> getDishItems() { return dishItems; }
    public void setDishItems(List<DishItem> dishItems) { this.dishItems = dishItems; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // Inner class for dish items
    public static class DishItem {
        @JsonProperty("dishId")
        private String dishId;

        @JsonProperty("dishImageUrl")
        private String dishImageUrl;

        @JsonProperty("dishName")
        private String dishName;

        @JsonProperty("dishPrice")
        private String dishPrice;

        @JsonProperty("dishQuantity")
        private int dishQuantity;

        // Getters and Setters
        public String getDishId() { return dishId; }
        public void setDishId(String dishId) { this.dishId = dishId; }

        public String getDishImageUrl() { return dishImageUrl; }
        public void setDishImageUrl(String dishImageUrl) { this.dishImageUrl = dishImageUrl; }

        public String getDishName() { return dishName; }
        public void setDishName(String dishName) { this.dishName = dishName; }

        public String getDishPrice() { return dishPrice; }
        public void setDishPrice(String dishPrice) { this.dishPrice = dishPrice; }

        public int getDishQuantity() { return dishQuantity; }
        public void setDishQuantity(int dishQuantity) { this.dishQuantity = dishQuantity; }
    }
}