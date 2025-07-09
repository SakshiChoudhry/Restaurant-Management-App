package com.restaurantapp.Service;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantapp.Controller.ApiResponse;
import com.restaurantapp.Controller.ReservationWaiterController;
import com.restaurantapp.Exception.ConflictException;
import com.restaurantapp.Exception.UnprocessableException;
import com.restaurantapp.Model.*;
import com.restaurantapp.Repository.ReservationRepoWaiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
public class ReservationWaiterService {
    private int visitor=0;
    private final ReservationRepoWaiter reservationRepoWaiter;
    private final List<TimeSlot> timeSlots;
    private final String orderTable=System.getenv("order_table");
    private final String waiterTable=System.getenv("waiter_table");
    private static final Logger log = LoggerFactory.getLogger(ReservationWaiterController.class);

    @Inject
    public ReservationWaiterService(ReservationRepoWaiter reservationRepoWaiter) {
        this.reservationRepoWaiter=reservationRepoWaiter;

        // Initialize time slots
        this.timeSlots = new ArrayList<>();
        this.timeSlots.add(new TimeSlot("1", "10:30 a.m.", "12:00 p.m."));
        this.timeSlots.add(new TimeSlot("2", "12:15 p.m.", "1:45 p.m."));
        this.timeSlots.add(new TimeSlot("3", "2:00 p.m.", "3:30 p.m."));
        this.timeSlots.add(new TimeSlot("4", "3:45 p.m.", "5:15 p.m."));
        this.timeSlots.add(new TimeSlot("5", "5:30 p.m.", "7:00 p.m."));
        this.timeSlots.add(new TimeSlot("6", "7:15 p.m.", "8:45 p.m."));
        this.timeSlots.add(new TimeSlot("7", "9:00 p.m.", "10:30 p.m."));
    }
    private String findSlotIdByStartTime(String startTime) {
        // Convert to 24-hour format for comparison
        String normalizedStartTime = normalizeTime(startTime);

        for (TimeSlot slot : timeSlots) {
            String slotStartTime = normalizeTime(slot.getSlotStartTime());
            if (slotStartTime.equals(normalizedStartTime)) {
                return slot.getSlotId();
            }
        }
        return "This time slot is not available";
    }
    private String findSlotIdByEndTime(String endTime) {
        // Convert to 24-hour format for comparison
        String normalizedStartTime = normalizeTime(endTime);

        for (TimeSlot slot : timeSlots) {
            String slotEndTime = normalizeTime(slot.getSlotEndTime());
            if (slotEndTime.equals(normalizedStartTime)) {
                return slot.getSlotId();
            }
        }
        return "This time slot is not available";
    }
    private String getTimeSlotString(String slotId) {
        for (TimeSlot slot : timeSlots) {
            if (slot.getSlotId().equals(slotId)) {
                return slot.getSlotStartTime() + " - " + slot.getSlotEndTime();
            }
        }
        return "Unknown time slot";
    }
    private String normalizeTime(String time) {
        // Handle different time formats
        if (time.contains("a.m.") || time.contains("p.m.")) {
            // Convert from "10:30 a.m." format to "10:30" or "22:30"
            String[] parts = time.split(" ");
            String timePart = parts[0];
            boolean isPM = parts[1].equals("p.m.");

            String[] hourMinute = timePart.split(":");
            int hour = Integer.parseInt(hourMinute[0]);

            if (isPM && hour < 12) {
                hour += 12;
            } else if (!isPM && hour == 12) {
                hour = 0;
            }

            return String.format("%02d:%s", hour, hourMinute[1]);
        }

        // Already in 24-hour format like "12:15"
        return time;
    }
    public String  fetchLocationIdFromWaiterTable(String waiterEmail){
        return reservationRepoWaiter.findLocationIdFromWaiterTable(waiterEmail);
    }
    public String  findStateOfOrder(String waiterEmail){
        return reservationRepoWaiter.findLocationIdFromWaiterTable(waiterEmail);
    }
    public void checkValidations(BookingWaiterRequest request){

        if (request.getClientType() == null || request.getClientType().trim().isEmpty()) {
            throw new IllegalArgumentException("Client type is required");
        }

        if (request.getLocationId() == null || request.getLocationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID is required");
        }
        if (request.getTableNumber() == null || request.getTableNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Table number is required");
        }
        if (Integer.parseInt(request.getTableNumber()) < 1 || Integer.parseInt(request.getTableNumber()) > 9) {
            throw new IllegalArgumentException("Table number must be between 1 and 9");
        }
        if (request.getDate() == null || request.getDate().trim().isEmpty()) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.getGuestsNumber() == null || request.getGuestsNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Number of guests is required");
        }
        if (Integer.parseInt(request.getGuestsNumber()) < 1) {
            throw new IllegalArgumentException("Number of guests must be greater than 0");
        }
        if (Integer.parseInt(request.getGuestsNumber()) > 11) {
            throw new IllegalArgumentException("Number of guests must be less than 11");
        }
        if (request.getTimeFrom() == null || request.getTimeFrom().trim().isEmpty()) {
            throw new IllegalArgumentException("Start time is required");
        }else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                    .withResolverStyle(ResolverStyle.STRICT);
            try{
                LocalDate.parse(request.getDate(), formatter);
                System.out.println("Date: " + request.getDate() + "is in valid format");
            }catch (DateTimeParseException e){
                throw new IllegalArgumentException("Date is not in valid format");
            }
        }
    }
//    private void validateBookingDate(String bookingDate) {
//        try {
//            // Parse the booking date
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            LocalDate parsedBookingDate = LocalDate.parse(bookingDate, formatter);
//
//            // Get current date
//            LocalDate currentDate = LocalDate.now();
//            LocalDate tomorrowDate = currentDate.plusDays(1L);
//
//            // Check if booking date is before tomorrow
//            if (parsedBookingDate.isBefore(tomorrowDate)) {
//                log.warn("Attempted to book for a past date: {}", bookingDate);
//                throw new IllegalArgumentException("Booking date cannot be in the past");
//            }
//
//            log.debug("Date validation passed for: {}", bookingDate);
//        } catch (DateTimeParseException e) {
//            log.error("Invalid date format: {}", bookingDate);
//            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd format");
//        }
//    }

//    private void validateBookingDate(String bookingDate, String startTime) {
//        try {
//            // Parse the booking date
//            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//            LocalDate parsedBookingDate = LocalDate.parse(bookingDate, dateFormatter);
//
//            // Get current date and time
//            LocalDate currentDate = LocalDate.now();
//            LocalTime currentTime = LocalTime.now();
//
//            // If booking is for today, check that the time is at least 30 minutes in the future
//            if (parsedBookingDate.isEqual(currentDate)) {
//                // Parse the start time (assuming format like "10:30 a.m.")
//                String normalizedTime = normalizeTime(startTime);
//                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
//                LocalTime bookingTime = LocalTime.parse(normalizedTime, timeFormatter);
//
//                // Check if booking time is at least 30 minutes after current time
//                if (bookingTime.isBefore(currentTime.plusMinutes(30))) {
//                    log.warn("Attempted to book for a time less than 30 minutes from now: {}", startTime);
//                    throw new IllegalArgumentException("Booking time must be at least 30 minutes from now");
//                }
//            }
//            // If booking is for a past date, reject it
//            else if (parsedBookingDate.isBefore(currentDate)) {
//                log.warn("Attempted to book for a past date: {}", bookingDate);
//                throw new IllegalArgumentException("Booking date cannot be in the past");
//            }
//
//            log.debug("Date validation passed for: {}", bookingDate);
//        } catch (DateTimeParseException e) {
//            log.error("Invalid date format: {}", bookingDate);
//            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd format");
//        }
//    }


//    private void validateBookingDate(String date, String timeFrom) {
//        try {
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
//            LocalDateTime reservationStart = LocalDateTime.parse(date + " " + timeFrom, formatter);
//            ZonedDateTime reservationStartIST = reservationStart.atZone(ZoneId.of("Asia/Kolkata"));
//            ZonedDateTime nowIST = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
//            if (reservationStartIST.isBefore(nowIST)) {
//                throw new IllegalArgumentException("Reservation cannot be made for a past time.");
//            }
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Invalid date/time format. Use yyyy-MM-dd and HH:mm");
//        }
//    }
private void validateBookingDateTime(String date, String timeFrom) {
    try {
        // Parse the date
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate bookingDate = LocalDate.parse(date, dateFormatter);

        // Parse and normalize the time from format like "12:15 p.m."
        String normalizedTime = normalizeTime(timeFrom);
        String[] timeParts = normalizedTime.split(":");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);

        // Create LocalDateTime for the reservation
        LocalDateTime reservationDateTime = LocalDateTime.of(bookingDate, LocalTime.of(hours, minutes));

        // Get current date and time in system default time zone
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

        // Create ZonedDateTime for the reservation in the same time zone
        ZonedDateTime reservationZDT = reservationDateTime.atZone(ZoneId.of("Asia/Kolkata"));

        // Check if reservation is in the past
        if (reservationZDT.isBefore(now)) {
            log.warn("Attempted to book for a past time: {}", timeFrom);
            throw new IllegalArgumentException("Booking time cannot be in the past");
        }

        // Check if reservation is less than 30 minutes from now
        if (reservationZDT.isBefore(now.plusMinutes(30))) {
            log.warn("Attempted to book for a time less than 30 minutes from now: {}", timeFrom);
            throw new IllegalArgumentException("Booking time must be at least 30 minutes from now");
        }

        log.debug("Date/time validation passed for: {} {}", date, timeFrom);
    } catch (DateTimeParseException e) {
        log.error("Invalid date format: {}", date);
        throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd format");
    } catch (IllegalArgumentException e) {
        // Re-throw IllegalArgumentException
        throw e;
    } catch (Exception e) {
        log.error("Error validating booking date and time: {}", e.getMessage());
        throw new IllegalArgumentException("Invalid booking time format. Please check your time format.");
    }
}
    public BookingWaiterResponse createReservationByWaiter(BookingWaiterRequest request, String waiterEmail){
        try{
            if(waiterEmail.equals(request.getCustomerEmail())){
                throw new IllegalArgumentException("Customer email and waiter email can not be same");
            }
            checkValidations(request);
            validateBookingDateTime(request.getDate(),request.getTimeFrom());
            String slotId = findSlotIdByStartTime(request.getTimeFrom());
            String slotIdEnd = findSlotIdByEndTime(request.getTimeTo());
            if(slotId.equals("This time slot is not available") || slotIdEnd.equals("This time slot is not available")){
                throw new Exception("This time slot is not available");
            }
            String locationId = fetchLocationIdFromWaiterTable(waiterEmail);
            if(!request.getLocationId().equals(locationId)){
                throw new IllegalArgumentException("Waiter is not assigned to the location.");
            }
            String tableId = reservationRepoWaiter.getTableId(request.getLocationId(), request.getTableNumber());

            if(tableId == null){
                throw new IllegalArgumentException("Table not found");
            }
            request.setTableNumber(tableId);
//            String slotId = "1";
            boolean isAlreadyBooked = reservationRepoWaiter.isTableAlreadyBooked(
                    request.getLocationId(),
                    request.getTableNumber(),
                    request.getDate(),
                    slotId
            );
//            boolean isAlreadyBooked=true;
            log.info("is checking booked {}",isAlreadyBooked);
            if (isAlreadyBooked) {
                throw new ConflictException("This table is already booked for the selected date and time slot");
            }

            if (waiterEmail == null) {
                log.warn("No waiters available for location: {}", request.getLocationId());
                throw new UnprocessableException("No waiters available for this location");
            }

            log.info("Assigned waiter {} to booking for location {} on date {}",
                    waiterEmail, request.getLocationId(), request.getDate());


            BookingWaiter booking = new BookingWaiter();
//          booking.setReservationId("101");
//            if(request.getClientType().equals("CUSTOMER")) {
//                booking.setStatus("Preorder");
//            }
//            else if(request.getClientType().equals("VISITOR")){
//                booking.setStatus("Reserved");
//            }
            booking.setStatus("Reserved");
            booking.setReservationId(UUID.randomUUID().toString());
            booking.setTableId(request.getTableNumber());
            if(request.getClientType().equals("CUSTOMER")) {
                booking.setCustomerEmail(request.getCustomerEmail());
            }
            else if(request.getClientType().equals("VISITOR")){
                booking.setCustomerEmail("visitor@gmail.com");
            }
            booking.setSlotId(slotId);
            booking.setDate(request.getDate());
            booking.setWaiterEmail(waiterEmail);
            booking.setLocationId(locationId);
            booking.setNumberOfGuests(request.getGuestsNumber());
            log.info("status is {}", booking.getStatus());
            log.info("resId is {}", booking.getReservationId());
            log.info("tableId is {}", booking.getTableId());
            log.info("cus email is {}", booking.getCustomerEmail());
            log.info("slotId is {}", booking.getSlotId());
            log.info("date is {}", booking.getDate());
            log.info("waiterEmail is {}", booking.getWaiterEmail());
            log.info("num of guests is {}", booking.getNumberOfGuests());
            log.info("loc id is {}", booking.getLocationId());

            // Save booking
//            BookingWaiter savedBooking = reservationRepoWaiter.createBooking(booking);

            // Get location address
            String locationAddress = reservationRepoWaiter.getLocationAddress(locationId);
            log.info("location Add is {}", locationAddress);
            if (locationAddress.equals("Unknown Location")) {
                throw new IllegalArgumentException("Invalid location ID");
            }
            log.info("customer email {}",request.getCustomerEmail());
            String userName="";
            if(request.getClientType().equals("CUSTOMER"))
            userName=reservationRepoWaiter.findUserNameFromEmail(request.getCustomerEmail());
//            log.info("passed usernsame {}",userName);
            String waiterName=reservationRepoWaiter.findUserNameFromEmail(waiterEmail);
            log.info("passed waitername {}",waiterName);
            BookingWaiter savedBooking = reservationRepoWaiter.createBooking(booking);
            // Create response
            BookingWaiterResponse response = new BookingWaiterResponse();
            response.setId(savedBooking.getReservationId());
            response.setStatus(savedBooking.getStatus());
            response.setLocationAddress(locationAddress);
            response.setDate(savedBooking.getDate());
            response.setTimeSlot(getTimeSlotString(slotId));
            response.setGuestsNumber(savedBooking.getNumberOfGuests());
            response.setTableNumber(request.getTableNumber());
            response.setFeedbackId("1");

//            response.setPreOrder("2");
            String state="";
            if(request.getClientType().equals("CUSTOMER"))
//            state=reservationRepoWaiter.findStatusOfOrder(request.getCustomerEmail());
            log.info("state is {}",state);
            String preOrderDishes="";
            if(request.getClientType().equals("CUSTOMER"))
            preOrderDishes=reservationRepoWaiter.findNumberOfDishesOfOrder(request.getCustomerEmail());
            log.info("preoder dish is {}",preOrderDishes);

//            else if(request.getClientType().equals("CUSTOMER") && state.equals("Preorder")) {
//                response.setPreOrder(preOrderDishes);
//            }
            if(request.getClientType().equals("CUSTOMER") ) {
                response.setPreOrder(preOrderDishes);
            }
            else {
                response.setPreOrder("0");
            }
            if(request.getClientType().equals("CUSTOMER")) {
                response.setUserInfo(request.getClientType() + " " + userName);
            }
            else if(request.getClientType().equals("VISITOR")){
                visitor++;
                response.setUserInfo(waiterName+" (Visitor "+visitor+")");
            }

            log.info("Booking created successfully: {}", savedBooking.getReservationId());
            return response;
            } catch (IllegalArgumentException e) {
                log.warn("Invalid booking request: {}", e.getMessage());
             throw new IllegalArgumentException(e.getMessage());
            } catch (ConflictException e) {
                // Let this bubble up untouched!
                log.warn("Conflict during booking: {}", e.getMessage());
                throw e;
            }  catch (UnprocessableException e) {
                log.warn("Unavaliability during booking: {}", e.getMessage());
                throw e;
            }catch (Exception e) {
                log.error("Error creating booking", e);
                throw new RuntimeException("Error creating booking: " + e.getMessage(), e);
        }
    }

    public String updateReservationByWaiter(UpdateRequestWaiter request) {
        String email=request.getCustomerEmail();
        String slotId = findSlotIdByStartTime(request.getTimeFrom());
        String slotIdEnd = findSlotIdByEndTime(request.getTimeTo());
        String tableNumber=request.getTableId();
        String date=request.getDate();
        String locationId=request.getLocationId();
        return reservationRepoWaiter.reservationRepoWaiter(email,slotId,tableNumber,locationId,date);
    }
}
