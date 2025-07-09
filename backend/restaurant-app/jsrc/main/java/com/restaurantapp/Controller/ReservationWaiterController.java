package com.restaurantapp.Controller;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurantapp.Exception.ConflictException;
import com.restaurantapp.Exception.UnprocessableException;
import com.restaurantapp.Model.*;
import com.restaurantapp.Service.ReservationWaiterService;
import org.openjdk.tools.sjavac.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class ReservationWaiterController {
    private static final Logger log = LoggerFactory.getLogger(ReservationWaiterController.class);
    private final ReservationWaiterService reservationWaiterService;
    private final ObjectMapper objectMapper;
    @Inject
    public ReservationWaiterController(ReservationWaiterService reservationWaiterService,ObjectMapper objectMapper){
        this.reservationWaiterService=reservationWaiterService;
        this.objectMapper=objectMapper;
    }

    public APIGatewayProxyResponseEvent createReservationByWaiter(String requestBody, Map<String, Object> claims){
//        log.info("waiter email is {}",waiterEmail);
        try{
            String waiterEmail = (String) claims.get("email");
            if (waiterEmail == null || waiterEmail.isEmpty()) {
                return ApiResponse.unauthorized("Waiter not authenticated properly");
            }
            BookingWaiterRequest request = objectMapper.readValue(requestBody, BookingWaiterRequest.class);

            BookingWaiterResponse bookingResponse=reservationWaiterService.createReservationByWaiter(request, waiterEmail);
            return ApiResponse.created(bookingResponse);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid booking request: {}", e.getMessage());
            if(e.getMessage().equals("Forbidden: Only waiters are authorized to create bookings using this API.")){
                return ApiResponse.forbidden("Forbidden: Only waiters are authorized to create bookings using this API.");
            }
            if(e.getMessage().equals("waiter is not assigned to the location.")){
                return ApiResponse.unprocessable("waiter is not assigned to the location.");
            }
            return ApiResponse.error(e.getMessage());
        } catch (ConflictException e) {
            log.warn("Booking conflict: {}", e.getMessage());
            return ApiResponse.conflict(e.getMessage());
        } catch (UnprocessableException e) {
            // Let this bubble up untouched!
            log.warn("Unavaliability during booking: {}", e.getMessage());
            return ApiResponse.unprocessable(e.getMessage());
        } catch (Exception e) {
        return ApiResponse.serverError("Error processing booking: " + e.getMessage());
        }
    }
    public APIGatewayProxyResponseEvent updateReservationByWaiter(String requestBody){
        try{
            UpdateRequestWaiter request = objectMapper.readValue(requestBody, UpdateRequestWaiter.class);
            String response=reservationWaiterService.updateReservationByWaiter(request);
            if(response.equals("Reservation Updated")){
                return ApiResponse.created(Map.of("message",response));
            }
            return ApiResponse.error("Error in reservation updation: Not Updated");
        }
        catch(Exception e){
            return ApiResponse.error("Error in reservation updation");
        }
    }
//    public void cancelReservationByWaiter(){
//
//    }
//    public List<ReservationResponseWaiter> getReservationsByLocation(){
//
//    }
}
