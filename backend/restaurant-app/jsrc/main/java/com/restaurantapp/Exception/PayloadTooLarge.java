package com.restaurantapp.Exception;

public class PayloadTooLarge extends RuntimeException {
    public PayloadTooLarge(String message) {
        super(message);
    }
}
