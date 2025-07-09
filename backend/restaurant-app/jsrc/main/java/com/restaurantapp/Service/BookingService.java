package com.restaurantapp.Service;

import com.restaurantapp.Exception.ConflictException;

import com.restaurantapp.Exception.UnprocessableException;
import com.restaurantapp.Model.*;
import com.restaurantapp.Repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

@Singleton
public class BookingService {
    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final List<TimeSlot> timeSlots;

    @Inject
    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;

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

    public BookingResponse createBooking(BookingRequest request, String customerEmail) {
        try {
            LOG.info("Creating booking for customer: {}", customerEmail);

            // Validate request
            if (request.getLocationId() == null || request.getLocationId().trim().isEmpty()) {
                throw new IllegalArgumentException("Location ID is required");
            }
            if (request.getTableNumber() == null || request.getTableNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Table number is required");
            }

            // Validate table exists at location
            if (!bookingRepository.isTableValidForLocation(request.getLocationId(), request.getTableNumber())) {
                throw new IllegalArgumentException("Invalid table number for this location");
            }

            if (request.getDate() == null || request.getDate().trim().isEmpty()) {
                throw new IllegalArgumentException("Date is required");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                        .withResolverStyle(ResolverStyle.STRICT);
                try {
                    LocalDate.parse(request.getDate(), formatter);
                    System.out.println("Date: " + request.getDate() + "is in valid format");
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Date is not in valid format");
                }
            }

            if (request.getGuestsNumber() == null || request.getGuestsNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("Number of guests is required");
            }

            try {
                int guests = Integer.parseInt(request.getGuestsNumber());
                if (guests < 1) {
                    throw new IllegalArgumentException("Number of guests must be greater than 0");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Number of guests must be a valid number");
            }

            // Validate table capacity
            if (!bookingRepository.hasTableSufficientCapacity(
                    request.getLocationId(),
                    request.getTableNumber(),
                    request.getGuestsNumber())) {
                throw new IllegalArgumentException("This table does not have sufficient capacity for " +
                        request.getGuestsNumber() + " guests");
            }

            if (request.getTimeFrom() == null || request.getTimeFrom().trim().isEmpty()) {
                throw new IllegalArgumentException("Start time is required");
            }

            // Validate date is not in the past
            validateBookingDate(request.getDate());

            // Find the slot ID based on the start time
            String slotId = findSlotIdByStartTime(request.getTimeFrom());
            String slotIdEnd = findSlotIdByEndTime(request.getTimeTo());
            if (slotId == null) {
                throw new IllegalArgumentException("Invalid start time cannot be more or less than 90 minutes");
            }
            if (slotIdEnd == null) {
                throw new IllegalArgumentException("Invalid end time cannot be more or less than 90 minutes");
            }

            if (!slotId.equals(slotIdEnd)) {
                throw new IllegalArgumentException("Can Only Book For 90 minutes within given slot");
            }

            // Get the table ID from the table number
            String tableId = bookingRepository.getTableId(request.getLocationId(), request.getTableNumber());
            if (tableId == null) {
                throw new IllegalArgumentException("Could not find table ID for the given table number");
            }

            // CHECK IF THE TABLE IS ALREADY BOOKED
            boolean isAlreadyBooked = bookingRepository.isTableAlreadyBooked(
                    request.getLocationId(),
                    tableId,
                    request.getDate(),
                    slotId
            );

            if (isAlreadyBooked) {
                throw new ConflictException("This table is already booked for the selected date and time slot");
            }

            // Get location address
            String tempAddressAryan = bookingRepository.getLocationAddress(request.getLocationId());

            if (tempAddressAryan.equals("Unknown Location")) {
                throw new IllegalArgumentException("Invalid location ID");
            }

            // Get a Algorithm for waiter
            String waiterEmail = bookingRepository.findAvailableWaiterForSlot(
                    request.getLocationId(),
                    request.getDate(),
                    slotId
            );
            if (waiterEmail == null) {
                LOG.warn("No waiters available for location: {}", request.getLocationId());
                throw new UnprocessableException("No waiters available for this location");
            }

            LOG.info("Assigned waiter {} to booking for location {} on date {}",
                    waiterEmail, request.getLocationId(), request.getDate());

            // Generate a secret code for feedback
            String secretCode = generateSecretCode();

            // Create booking entity
            Booking booking = new Booking();
            booking.setTableId(tableId);  // Use the tableId from the database, not the table number
            booking.setCustomerEmail(customerEmail);
            booking.setSlotId(slotId);
            booking.setDate(request.getDate());
            booking.setWaiterEmail(waiterEmail);
            booking.setLocationId(request.getLocationId());
            booking.setNumberOfGuests(request.getGuestsNumber());
            booking.setSecretCode(secretCode);

            // Save booking
            Booking savedBooking = bookingRepository.createBooking(booking);

            // Get location address
            String locationAddress = bookingRepository.getLocationAddress(request.getLocationId());

            if (locationAddress.equals("Unknown Location")) {
                throw new IllegalArgumentException("Invalid location ID");
            }

            // Create response
            BookingResponse response = new BookingResponse();
            response.setId(savedBooking.getReservationId());
            response.setStatus(savedBooking.getStatus());
            response.setLocationAddress(locationAddress);
            response.setDate(savedBooking.getDate());
            response.setTimeSlot(getTimeSlotString(slotId));
            response.setGuestsNumber(savedBooking.getNumberOfGuests());

            LOG.info("Booking created successfully: {}", savedBooking.getReservationId());
            return response;
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid booking request: {}", e.getMessage());
            throw e;
        } catch (ConflictException e) {
            // Let this bubble up untouched!
            LOG.warn("Conflict during booking: {}", e.getMessage());
            throw e;
        } catch (UnprocessableException e) {
            // Let this bubble up untouched!
            LOG.warn("Unavaliability during booking: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error creating booking", e);
            throw new RuntimeException("Error creating booking: " + e.getMessage(), e);
        }
    }

    /**
     * Generate a random secret code for feedback
     * @return A 6-character alphanumeric code
     */
    private String generateSecretCode() {
        // Generate a random 6-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    /**
     * Validates that the booking date is not in the past
     * @param bookingDate The date to validate in format "yyyy-MM-dd"
     * @throws IllegalArgumentException if the date is in the past
     */
    private void validateBookingDate(String bookingDate) {
        try {
            // Parse the booking date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate parsedBookingDate = LocalDate.parse(bookingDate, formatter);

            // Get current date
            LocalDate cr = LocalDate.now();
            LocalDate currentDate = cr.plusDays(1L);

            // Check if booking date is after current date
            if (parsedBookingDate.isBefore(currentDate)) {
                LOG.warn("Attempted to book for a past date: {}", bookingDate);
                throw new IllegalArgumentException("Booking date cannot be in the past");
            }

            LOG.debug("Date validation passed for: {}", bookingDate);
        } catch (DateTimeParseException e) {
            LOG.error("Invalid date format: {}", bookingDate);
            throw new IllegalArgumentException("Invalid date format. Please use yyyy-MM-dd format");
        }
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
        return null;
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
        return null;
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

    private String getTimeSlotString(String slotId) {
        for (TimeSlot slot : timeSlots) {
            if (slot.getSlotId().equals(slotId)) {
                return slot.getSlotStartTime() + " - " + slot.getSlotEndTime();
            }
        }
        return "Unknown time slot";
    }

    private String getRandomWaiter() {
        List<String> waiters = bookingRepository.getWaiterEmails();
        if (waiters.isEmpty()) {
            return null;
        }

        Random random = new Random();
        int index = random.nextInt(waiters.size());
        return waiters.get(index);
    }



    public BookingResponse updateBooking(String reservationId, BookingUpdateRequest request, String customerEmail) {
        try {
            LOG.info("Updating booking {} for customer: {}", reservationId, customerEmail);

            // Get the existing booking
            Booking existingBooking = bookingRepository.getBookingById(reservationId);

            if (existingBooking == null) {
                throw new IllegalArgumentException("Booking not found with ID: " + reservationId);
            }

            // Verify that the booking belongs to the customer
            if (!existingBooking.getCustomerEmail().equals(customerEmail)) {
                throw new IllegalArgumentException("You are not authorized to update this booking");
            }

            // Check if the booking status allows updates
            if (!"Reserved".equals(existingBooking.getStatus())) {
                throw new IllegalArgumentException("This booking cannot be updated because its status is: " + existingBooking.getStatus());
            }

            // Validate date if provided
            String newDate = request.getDate();
            if (newDate != null && !newDate.trim().isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                        .withResolverStyle(ResolverStyle.STRICT);
                try {
                    LocalDate.parse(newDate, formatter);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Date is not in valid format");
                }

                // Validate date is not in the past
                validateBookingDate(newDate);
            } else {
                // Use existing date if not provided
                newDate = existingBooking.getDate();
            }

            // Validate number of guests if provided
            String newGuestsNumber = request.getGuestsNumber();
            if (newGuestsNumber != null && !newGuestsNumber.trim().isEmpty()) {
                try {
                    int guests = Integer.parseInt(newGuestsNumber);
                    if (guests < 1) {
                        throw new IllegalArgumentException("Number of guests must be greater than 0");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Number of guests must be a valid number");
                }
            } else {
                // Use existing number of guests if not provided
                newGuestsNumber = existingBooking.getNumberOfGuests();
            }

            // Find the slot ID based on the start time if provided
            String newSlotId = existingBooking.getSlotId();
            if (request.getTimeFrom() != null && !request.getTimeFrom().trim().isEmpty()) {
                String slotId = findSlotIdByStartTime(request.getTimeFrom());
                if (slotId == null) {
                    throw new IllegalArgumentException("Invalid start time");
                }

                // If end time is also provided, validate it
                if (request.getTimeTo() != null && !request.getTimeTo().trim().isEmpty()) {
                    String slotIdEnd = findSlotIdByEndTime(request.getTimeTo());
                    if (slotIdEnd == null) {
                        throw new IllegalArgumentException("Invalid end time");
                    }

                    if (!slotId.equals(slotIdEnd)) {
                        throw new IllegalArgumentException("Can Only Book For 90 minutes within given slot");
                    }
                }

                newSlotId = slotId;
            }

            // Get the table ID from the existing booking
            String tableId = existingBooking.getTableId();
            String locationId = existingBooking.getLocationId();

            // Validate table capacity for the new number of guests
            if (!bookingRepository.hasTableSufficientCapacity(
                    locationId,
                    bookingRepository.getTableNumber(locationId, tableId), // We need to add this method to get table number from tableId
                    newGuestsNumber)) {
                throw new IllegalArgumentException("This table does not have sufficient capacity for " +
                        newGuestsNumber + " guests");
            }

            // Check if the table is already booked for the new date and time slot (excluding current booking)
            boolean isAlreadyBooked = bookingRepository.isTableAlreadyBookedExcludingCurrent(
                    locationId,
                    tableId,
                    newDate,
                    newSlotId,
                    reservationId
            );

            if (isAlreadyBooked) {
                throw new ConflictException("This table is already booked for the selected date and time slot");
            }

            // If date or time slot changed, we may need to reassign a waiter
            String waiterEmail = existingBooking.getWaiterEmail();
            if (!newDate.equals(existingBooking.getDate()) || !newSlotId.equals(existingBooking.getSlotId())) {
                // Get a new waiter for the new date and time slot
                waiterEmail = bookingRepository.findAvailableWaiterForSlot(
                        locationId,
                        newDate,
                        newSlotId
                );

                if (waiterEmail == null) {
                    LOG.warn("No waiters available for location: {}", locationId);
                    throw new UnprocessableException("No waiters available for this location at the requested time");
                }
            }

            // Update the booking
            existingBooking.setDate(newDate);
            existingBooking.setSlotId(newSlotId);
            existingBooking.setNumberOfGuests(newGuestsNumber);
            existingBooking.setWaiterEmail(waiterEmail);

            // Save the updated booking
            Booking updatedBooking = bookingRepository.updateBooking(existingBooking);

            // Get location address
            String locationAddress = bookingRepository.getLocationAddress(locationId);

            // Create response
            BookingResponse response = new BookingResponse();
            response.setId(updatedBooking.getReservationId());
            response.setStatus(updatedBooking.getStatus());
            response.setLocationAddress(locationAddress);
            response.setDate(updatedBooking.getDate());
            response.setTimeSlot(getTimeSlotString(newSlotId));
            response.setGuestsNumber(updatedBooking.getNumberOfGuests());

            LOG.info("Booking updated successfully: {}", updatedBooking.getReservationId());
            return response;
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid booking update request: {}", e.getMessage());
            throw e;
        } catch (ConflictException e) {
            LOG.warn("Conflict during booking update: {}", e.getMessage());
            throw e;
        } catch (UnprocessableException e) {
            LOG.warn("Unavailability during booking update: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error updating booking", e);
            throw new RuntimeException("Error updating booking: " + e.getMessage(), e);
        }
    }
}