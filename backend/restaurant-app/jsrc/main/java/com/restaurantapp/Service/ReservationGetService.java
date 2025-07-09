package com.restaurantapp.Service;

import com.restaurantapp.Model.*;
import com.restaurantapp.Repository.ReservationGetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ReservationGetService
{
    int visitor=0;
    private static final Logger LOG = LoggerFactory.getLogger(ReservationGetService.class);

    private final ReservationGetRepository reservationRepository;

    @Inject
    public ReservationGetService(ReservationGetRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /**
     * Gets all reservations for a specific user.
     *
     * @param email The user's email
     * @return List of reservation responses
     */
    public List<ReservationGetResponse> getReservationsForUser(String email)
    {
        try {
            LOG.info("Getting reservations for user: {}", email);

            // Log environment variables for debugging
            LOG.info("Environment variables:");
            LOG.info("reservation_table: {}",
                    System.getenv("reservation_table"));
//            LOG.info("slot_table: {}", System.getenv("slot_table"));
            LOG.info("location_table: {}", System.getenv("location_table"));

            List<ReservationGet> reservations = reservationRepository.findReservationsByEmail(email);
            LOG.info("Raw reservations found: {}", reservations.size());


            LOG.info("Raw reservations are {}",reservations);
            // Log the first reservation if available
//            if (!reservations.isEmpty()) {
//                ReservationGet firstRes = reservations.get(0);
////                LOG.debug("First reservation: id={}, email={}, locationId={}, slotId={}, tableId={}, date={}",
////                        firstRes.getReservationId(),
////                        firstRes.getCustomerEmail(),
////                        firstRes.getLocationId(),
////                        firstRes.getSlotId(),
////                        firstRes.getTableId(),
////                        firstRes.getDate());  // Added date logging
//                firstRes.getStatus();
//            }

            List<ReservationGetResponse> responses = new ArrayList<>();

            for (ReservationGet reservation : reservations)
            {
                try {
                    ReservationGetResponse response = new ReservationGetResponse();

                    // Set reservation ID
                    response.setId(reservation.getReservationId());

                    // Set status
                    response.setStatus(reservation.getStatus());

                    // Set guests number
                    response.setGuestsNumber(String.valueOf(reservation.getNumberOfGuests()));
                    response.setTableNumber(reservation.getTableNumber());


                    // Set default values
                    String preOrderDishes="";
                    String customerEmail=reservation.getCustomerEmail();
                    if(customerEmail.equals("visitor@gmail.com")) {
                        preOrderDishes = "0";
                    }
                    else {
                        preOrderDishes = reservationRepository.findNumberOfDishesOfOrder(customerEmail);
                        if(preOrderDishes.equals("No orders found")){
                            preOrderDishes="0";
                        }
                    }

                    response.setPreOrder(preOrderDishes);
                    response.setFeedbackId("1");


                    String userName=reservationRepository.findUserNameFromEmail(customerEmail);
                    String waiterName=reservationRepository.findUserNameFromEmail(email);
                    if(!customerEmail.equals("visitor@gmail.com")) {
                        response.setUserInfo(userName);
                    } else if(customerEmail.equals("visitor@gmail.com")){
                        visitor++;
                        response.setUserInfo(waiterName+" (Visitor "+visitor+")");
                    }

                    String locationId = reservationRepository.findLocationIdFromWaiterTable(email);
                    String locationAddress = reservationRepository.getLocationAddress(locationId);
                    response.setLocationAddress(locationAddress);

                    // Get time slot from Slot table
                    if (reservation.getSlotId() != null)
                    {
                        try {
                            Slot slot = reservationRepository.getSlotById(reservation.getSlotId());
                            if (slot != null) {
                                // Format time slot
                                String timeSlot = formatTimeSlot(slot.getStartTime(), slot.getEndTime());
                                response.setTimeSlot(timeSlot);
                                LOG.debug("Found time slot: {} for reservation: {}",
                                        timeSlot, reservation.getReservationId());
                            } else {
                                LOG.warn("Slot not found for ID: {}", reservation.getSlotId());
                                response.setTimeSlot("Time not available");
                            }
                        } catch (Exception e) {
                            LOG.warn("Error getting slot for ID {}: {}",
                                    reservation.getSlotId(), e.getMessage());
                            response.setTimeSlot("Error retrieving time slot");
                        }
                    } else {
                        response.setTimeSlot("Time not specified");
                    }

                    // Get date directly from Reservation
                    // UPDATED: Get date from reservation object instead of table lookup
                    if (reservation.getDate() != null && !reservation.getDate().isEmpty()) {
                        response.setDate(reservation.getDate());
                        LOG.debug("Found date: {} for reservation: {}",
                                reservation.getDate(), reservation.getReservationId());
                    } else {
                        // Fallback: Try to extract date from slot's start time if available
                        try {
                            if (reservation.getSlotId() != null) {
                                Slot slot = reservationRepository.getSlotById(reservation.getSlotId());
                                if (slot != null && slot.getStartTime() != null && slot.getStartTime().contains("T")) {
                                    String date = slot.getStartTime().split("T")[0];
                                    response.setDate(date);
                                    LOG.debug("Extracted date from slot: {} for reservation: {}",
                                            date, reservation.getReservationId());
                                } else {
                                    response.setDate("Date not available");
                                }
                            } else {
                                response.setDate("Date not available");
                            }
                        } catch (Exception e) {
                            LOG.warn("Error extracting date from slot: {}", e.getMessage());
                            response.setDate("Error retrieving date");
                        }
                    }

                    responses.add(response);
                    LOG.info("response is after getting {}",response);
                    LOG.info("all responses are after getting {}",responses);
                } catch (Exception e) {
                    LOG.error("Error processing reservation {}: {}",
                            reservation.getReservationId(), e.getMessage(), e);
                    // Continue with next reservation instead of failing completely
                }
            }

            LOG.info("Found {} reservations for user: {}", responses.size(), email);
            return responses;
        }
        catch (Exception e) {
            LOG.error("Error getting reservations for user: {}", email, e);
            throw new RuntimeException("Error getting reservations for user: " + e.getMessage(), e);
        }
    }


    private String formatTimeSlot(String startTime, String endTime)
    {
        try {
            if (startTime == null || endTime == null) {
                return "Time not available";
            }

            // Extract time portion from ISO format
            String startTimeFormatted = startTime.contains("T") ?
                    startTime.split("T")[1].substring(0, 5) : startTime;

            String endTimeFormatted = endTime.contains("T") ?
                    endTime.split("T")[1].substring(0, 5) : endTime;

            return startTimeFormatted + " - " + endTimeFormatted;
        } catch (Exception e) {
            LOG.warn("Error formatting time slot from startTime: {} and endTime: {}", startTime, endTime, e);
            return "Unknown time slot";
        }
    }
}
