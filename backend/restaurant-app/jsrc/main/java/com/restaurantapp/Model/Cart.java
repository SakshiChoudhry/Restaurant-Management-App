package com.restaurantapp.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Cart {
    @JsonProperty("id")
    private String id;

    @JsonProperty("customerEmail")
    private String customerEmail;

    @JsonProperty("reservationId")
    private String reservationId;

    @JsonProperty("date")
    private String date;

    @JsonProperty("timeSlot")
    private String timeSlot;

    @JsonProperty("address")
    private String address;

    @JsonProperty("state")
    private String state;

    @JsonProperty("locationId")
    private String locationId;

    @JsonProperty("dishItems")  // This will be serialized as dishItems in the response
    private List<OrderItem> orderItems;

    // Constructors
    public Cart() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }
}