package com.restaurantapp.Model;

/**
 * Model class for simplified dish responses in list views
 */
public class DishResponse {
    private String id;
    private String name;
    private String previewImageUrl;
    private String price;
    private String state;
    private String weight;

    public DishResponse() {
    }

    // Constructor
    public DishResponse(String id, String name, String previewImageUrl, String price, String state, String weight) {
        this.id = id;
        this.name = name;
        this.previewImageUrl = previewImageUrl;
        this.price = price;
        this.state = state;
        this.weight = weight;
    }

    // Factory method to convert from Dish to DishResponse
    public static DishResponse fromDish(Dish dish) {
        return new DishResponse(
                dish.getDishId(),
                dish.getDishName(),
                dish.getImageUrl(),
                dish.getPrice(),
                dish.isState() ? "Available" : "On stop",
                dish.getWeight()
        );
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
}