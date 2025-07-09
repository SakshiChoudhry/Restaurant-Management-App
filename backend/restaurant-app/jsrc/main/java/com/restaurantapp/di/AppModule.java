package com.restaurantapp.di;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Controller.*;
import com.restaurantapp.Middleware.AuthMiddleware;
import com.restaurantapp.Repository.*;
import com.restaurantapp.Service.*;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for providing dependencies.
 */
@Module
public class AppModule {
    @Provides
    @Singleton
    public CustomerFeedbackRepository provideCustomerFeedbackRepository() {
        return new CustomerFeedbackRepository();
    }

    @Provides
    @Singleton
    public CustomerFeedbackService provideCustomerFeedbackService(CustomerFeedbackRepository customerFeedbackRepository) {
        return new CustomerFeedbackService(customerFeedbackRepository);
    }

    @Provides
    @Singleton
    public CustomerFeedbackController provideCustomerFeedbackController(CustomerFeedbackService customerFeedbackService , ObjectMapper objectMapper) {
        return new CustomerFeedbackController(customerFeedbackService, objectMapper);
    }


    // US 11
    @Singleton
    @Provides
    public AnonymousFeedbackService provideAnonymousFeedbackService(
            BookingRepository bookingRepository,
            WaiterRepository waiterRepository) {
        return new AnonymousFeedbackService(bookingRepository, waiterRepository);
    }
    @Singleton
    @Provides
    public WaiterRepository provideWaiterRepository() {
        return new WaiterRepository();
    }
    @Singleton
    @Provides
    public AnonymousFeedbackController provideAnonymousFeedbackController(AnonymousFeedbackService feedbackService) {
        return new AnonymousFeedbackController(feedbackService);
    }

    //us9
    @Provides
    @Singleton
    public DishService provideDishService(DishRepository dishRepository) {
        return new DishService(dishRepository);
    }

    @Provides
    @Singleton
    public DishController provideDishController(DishService dishService, ObjectMapper objectMapper) {
        return new DishController(dishService, objectMapper);
    }
    //
    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

    @Provides
    @Singleton
    public GreetingService provideGreetingService() {
        return new GreetingService();
    }

    @Provides
    @Singleton
    public CognitoService provideCognitoService() {
        return new CognitoService();
    }

    @Provides
    @Singleton
    public UserRepository provideUserRepository() {
        return new UserRepository();
    }

    @Provides
    @Singleton
    public UserService provideUserService(UserRepository userRepository, CognitoService cognitoService) {
        return new UserService(userRepository, cognitoService);
    }

    @Provides
    @Singleton
    public DemoController provideDemoController(GreetingService greetingService) {
        return new DemoController(greetingService);
    }

    @Provides
    @Singleton
    public UserController provideUserController(UserService userService, ObjectMapper objectMapper) {
        return new UserController(userService, objectMapper);
    }

    @Provides
    @Singleton
    public ProfileController provideProfileController(UserService userService) {
        return new ProfileController(userService);
    }

    @Provides
    @Singleton
    public AuthMiddleware provideAuthMiddleware(CognitoService cognitoService) {
        return new AuthMiddleware(cognitoService);
    }

    @Provides
    @Singleton
    public BookingRepository provideBookingRepository() {
        return new BookingRepository();
    }

    @Provides
    @Singleton
    public BookingService provideBookingService(BookingRepository bookingRepository) {
        return new BookingService(bookingRepository);
    }

    @Provides
    @Singleton
    public BookingController provideBookingController(BookingService bookingService, ObjectMapper objectMapper) {
        return new BookingController(bookingService, objectMapper);
    }
    @Provides
    @Singleton
    public ReservationDeletionRepository provideReservationDeletionRepository() {
        return new ReservationDeletionRepository();
    }

    @Provides
    @Singleton
    public ReservationDeletionService provideReservationDeletionService(ReservationDeletionRepository reservationDeletionRepository) {
        return new ReservationDeletionService(reservationDeletionRepository);
    }

    @Provides
    @Singleton
    public ReservationDeletionController provideReservationDeletionController(ReservationDeletionService reservationDeletionService, ObjectMapper objectMapper) {
        return new ReservationDeletionController(reservationDeletionService, objectMapper);
    }

    //nan
    @Provides
    @Singleton
    public ReservationRepository provideReservationRepository() {
        return new ReservationRepository();
    }

    @Provides
    @Singleton
    public ReservationService provideReservationService(ReservationRepository reservationRepository)
    {
        return new ReservationService(reservationRepository);
    }

    @Provides
    @Singleton
    public ReservationController provideReservationController(ReservationService reservationService,ObjectMapper objectMapper) {
        return new ReservationController(reservationService,objectMapper);
    }


    // Dishes and Feedback

    @Provides
    @Singleton
    public DishesRepository provideDishesRepository() {
        return new DishesRepository();
    }

    @Provides
    @Singleton
    public FeedbackRepository provideFeedbackRepository() {
        return new FeedbackRepository();
    }


    @Provides
    @Singleton
    public DishesService provideDishesService(DishesRepository dishesRepository) {
        return new DishesService(dishesRepository);
    }

    @Provides
    @Singleton
    public FeedbackService provideFeedbackService(FeedbackRepository feedbackRepository) {
        return new FeedbackService(feedbackRepository);
    }

    @Provides
    @Singleton
    public DishesController provideDishesController(DishesService dishesService, ObjectMapper objectMapper) {
        return new DishesController(dishesService, objectMapper);
    }

    @Provides
    @Singleton
    public FeedbackController provideFeedbackController(FeedbackService feedbackService) {
        return new FeedbackController(feedbackService);
    }

    //TABLES

    @Provides
    @Singleton
    public TableAvailabilityRepository provideTableAvailabilityRepository() {
        return new TableAvailabilityRepository();
    }

    @Provides
    @Singleton
    public RestaurantLocationRepository provideLocationRepository() {
        return new RestaurantLocationRepository();
    }

    @Provides
    @Singleton
    public TableService provideTableService() {
        return new TableService();
    }

    @Provides
    @Singleton
    public TableController provideTableController() {
        return new TableController();
    }

    @Provides
    @Singleton
    public CartRepository provideCartRepository() {
        return new CartRepository();
    }

    @Provides
    @Singleton
    public OrderRepository provideOrderRepository() {
        return new OrderRepository();
    }

    @Provides
    @Singleton
    public CartService provideCartService(CartRepository cartRepository, OrderRepository orderRepository, DishesRepository dishesRepository) {
        return new CartService(cartRepository, orderRepository, dishesRepository);
    }

    @Provides
    @Singleton
    public CartController provideCartController(CartService cartService, ObjectMapper objectMapper) {
        return new CartController(cartService, objectMapper);
    }
}