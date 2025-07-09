package com.restaurantapp.Exception;

public class UnprocessableException extends RuntimeException{
    public UnprocessableException(String message){
        super(message);
    }
}
