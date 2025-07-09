package com.restaurantapp.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Controller.*;
import com.restaurantapp.Middleware.AuthMiddleware;
import com.restaurantapp.Service.ReservationService;
import dagger.Component;

import javax.inject.Singleton;

/**
 * Dagger component for dependency injection.
 */
@Singleton
@Component(modules = {AppModule.class})
public interface AppComponent {
    DemoController demoController();
    UserController userController();
    ProfileController profileController();
    AuthMiddleware authMiddleware();
    ObjectMapper objectMapper();
    // Add this method to your Booking
    BookingController bookingController();
    ReservationGetController reservationGetController();
    //TABLE CONTROLLER
    TableController tableController();

    //Dishes and Feedback Controller
    DishesController dishesController();
    FeedbackController feedbackController();
    ReservationWaiterController reservationWaiterController();
    CartController cartController();
    DishController dishController();


    //US 11
    AnonymousFeedbackController anonymousFeedbackController();

    //reservation
    ReservationService reservationService();
    ReservationDeletionController reservationDeletionController();
    ReservationController reservationController();

    //us15
    CustomerFeedbackController customerFeedbackController();
}