package com.restaurantapp.Model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Model class for detailed dish responses in single dish view
 */
@JsonPropertyOrder({
        "calories",
        "carbohydrates",
        "description",
        "dishType",
        "fats",
        "id",
        "imageUrl",
        "name",
        "price",
        "proteins",
        "state",
        "vitamins",
        "weight"
})
public class DishResponseById {
    private String calories;
    private String carbohydrates;
    private String description;
    private String dishType;
    private String fats;
    private String id;
    private String imageUrl;
    private String name;
    private String price;
    private String proteins;
    private String state;
    private String vitamins;
    private String weight;

    // Default constructor
    public DishResponseById() {
    }

    // Factory method to convert from Dish to DishResponseById
    public static DishResponseById fromDish(Dish dish) {
        DishResponseById response = new DishResponseById();

        response.setCalories(dish.getCalories());
        response.setCarbohydrates(dish.getCarbohydrates());
        response.setDescription(dish.getDishDescription());
        response.setDishType(dish.getDishType());
        response.setFats(dish.getFats());
        response.setId(dish.getDishId());
        response.setImageUrl(dish.getImageUrl());
        response.setName(dish.getDishName());
        response.setPrice(dish.getPrice());
        response.setProteins(dish.getProteins());
        response.setState(dish.isState() ? "Available" : "On Stop");
        response.setVitamins(dish.getVitamins());
        response.setWeight(dish.getWeight());

        return response;
    }

    // Getters and Setters
    public String getCalories() {
        return calories;
    }

    public void setCalories(String calories) {
        this.calories = calories;
    }

    public String getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(String carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDishType() {
        return dishType;
    }

    public void setDishType(String dishType) {
        this.dishType = dishType;
    }

    public String getFats() {
        return fats;
    }

    public void setFats(String fats) {
        this.fats = fats;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getProteins() {
        return proteins;
    }

    public void setProteins(String proteins) {
        this.proteins = proteins;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getVitamins() {
        return vitamins;
    }

    public void setVitamins(String vitamins) {
        this.vitamins = vitamins;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }
}