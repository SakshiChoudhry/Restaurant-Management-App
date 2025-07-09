package com.restaurantapp.Service;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GreetingService {

    @Inject
    public GreetingService() {
        // Constructor injection
    }

    public String getGreeting(String name) {
        return "Hello, " + name + "!";
    }
}