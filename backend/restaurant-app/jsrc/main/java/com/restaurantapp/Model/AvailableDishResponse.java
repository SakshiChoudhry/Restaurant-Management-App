package com.restaurantapp.Model;

public class AvailableDishResponse {
    private String dishId;
    private String dishName;
    private String imageUrl;
    private String price;
    private String state;
    private String weight;

    public AvailableDishResponse() {}

    public AvailableDishResponse(String dishId, String dishName, String imageUrl,
                                 String price, String state, String weight) {
        this.dishId = dishId;
        this.dishName = dishName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.state = state;
        this.weight = weight;
    }

    // Getters and Setters
    public String getDishId() { return dishId; }
    public void setDishId(String dishId) { this.dishId = dishId; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
}