package com.restaurantapp.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItem {
    @JsonProperty("dishId")
    private String dishId;

    @JsonProperty("dishName")
    private String dishName;

    @JsonProperty("dishImageUrl")
    private String dishImageUrl;

    @JsonProperty("dishPrice")
    private String dishPrice;

    @JsonProperty("dishQuantity")  // This will be serialized as dishQuantity in the response
    private int orderQuantity;

    // Constructors
    public OrderItem() {}

    // Getters and Setters
    public String getDishId() { return dishId; }
    public void setDishId(String dishId) { this.dishId = dishId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getDishImageUrl() { return dishImageUrl; }
    public void setDishImageUrl(String dishImageUrl) { this.dishImageUrl = dishImageUrl; }

    public String getDishPrice() { return dishPrice; }
    public void setDishPrice(String dishPrice) { this.dishPrice = dishPrice; }

    public int getOrderQuantity() { return orderQuantity; }
    public void setOrderQuantity(int orderQuantity) { this.orderQuantity = orderQuantity; }
}