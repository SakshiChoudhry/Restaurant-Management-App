package com.restaurantapp.Controller;

import com.restaurantapp.Service.GreetingService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Controller for demonstration endpoints.
 */
@Singleton
public class DemoController {
    private final GreetingService greetingService;

    @Inject
    public DemoController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * Returns a greeting message.
     *
     * @return A greeting message
     */
    public String hello() {
        return greetingService.getGreeting("Aryan Vedy");
    }
}