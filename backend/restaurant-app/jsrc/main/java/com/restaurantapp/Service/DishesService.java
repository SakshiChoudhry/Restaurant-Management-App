package com.restaurantapp.Service;

import com.restaurantapp.Model.AvailableDishResponse;
import com.restaurantapp.Model.Dish;
import com.restaurantapp.Model.PopularDishesResponse;
import com.restaurantapp.Repository.DishesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for user management operations.
 */
@Singleton
public class DishesService {
    private static final Logger LOG = LoggerFactory.getLogger(DishesService.class);

    private final DishesRepository dishesRepository;

    @Inject
    public DishesService(DishesRepository dishesRepository) {
        this.dishesRepository = dishesRepository;
    }

    /**
     * Registers a new user.
     *
     * @return Result message
     */
    public List<PopularDishesResponse> getPopularDishes() {
        try {
            LOG.info("Retrieving popular dishes from DynamoDB");

            // Use a repository method to fetch popular dishes from DynamoDB
            List<PopularDishesResponse> popularDishes = dishesRepository.getPopularDishes();

            LOG.info("Successfully retrieved {} popular dishes from DynamoDB", popularDishes.size());
            return popularDishes;

        } catch (Exception e) {
            LOG.error("Error retrieving popular dishes from DynamoDB", e);
            // Return an empty list on error
            return new ArrayList<>();
            // Or throw a runtime exception if you prefer
            // throw new RuntimeException("Failed to retrieve popular dishes from DynamoDB", e);
        }
    }

    // Service method to get available dishes for reservationId
    public List<AvailableDishResponse> getAvailableDishesForReservation(String reservationId) {
        LOG.info("Getting available dishes for reservation: {}", reservationId);

        String locationId = dishesRepository.getLocationIdFromReservation(reservationId);
        LOG.info("Found locationId: {} for reservation: {}", locationId, reservationId);

        if (locationId == null) {
            LOG.error("Reservation not found or locationId missing for reservationId: {}", reservationId);
            throw new IllegalArgumentException("Reservation not found or locationId missing for reservationId: " + reservationId);
        }

        List<Dish> dishes = dishesRepository.getAvailableDishesByLocation(locationId);
        LOG.info("Found {} available dishes for location: {}", dishes.size(), locationId);

        return dishes.stream()
                .map(dish -> new AvailableDishResponse(
                        dish.getDishId(),
                        dish.getDishName(),
                        dish.getImageUrl(),
                        dish.getPrice(),
                        dish.isState() ? "Available" : "On Stop",
                        dish.getWeight()))
                .collect(Collectors.toList());
    }

    public Dish getDishById(String dishId) {
        return dishesRepository.getDishById(dishId);
    }


}