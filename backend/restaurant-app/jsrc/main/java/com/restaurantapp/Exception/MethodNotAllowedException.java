package com.restaurantapp.Exception;

public class MethodNotAllowedException extends RuntimeException{
    public MethodNotAllowedException(String message){
        super(message);
    }
}
