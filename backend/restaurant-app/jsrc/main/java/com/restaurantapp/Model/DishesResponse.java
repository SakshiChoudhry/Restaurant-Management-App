package com.restaurantapp.Model;

import java.util.List;

public class DishesResponse {
    private List<DishResponse> content;

    public DishesResponse() {
    }

    public DishesResponse(List<DishResponse> content) {
        this.content = content;
    }

    public List<DishResponse> getContent() {
        return content;
    }

    public void setContent(List<DishResponse> content) {
        this.content = content;
    }
}