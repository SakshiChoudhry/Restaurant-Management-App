package com.restaurantapp.Service;

import com.restaurantapp.Exception.ConflictException;
import com.restaurantapp.Exception.UnprocessableException;
import com.restaurantapp.Model.TimeSlot;
import com.restaurantapp.Repository.ReservationDeletionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class ReservationDeletionService {
    private static final Logger LOG = LoggerFactory.getLogger(ReservationDeletionService.class);
    private final ReservationDeletionRepository reservationDeletionRepository;
    private final List<TimeSlot> timeSlots;

    @Inject
    public ReservationDeletionService(ReservationDeletionRepository reservationDeletionRepository) {
        this.reservationDeletionRepository = reservationDeletionRepository;

        // time slots:
        this.timeSlots = new ArrayList<>();
        this.timeSlots.add(new TimeSlot("1", "10:30 a.m.", "12:00 p.m."));
        this.timeSlots.add(new TimeSlot("2", "12:15 p.m.", "1:45 p.m."));
        this.timeSlots.add(new TimeSlot("3", "2:00 p.m.", "3:30 p.m."));
        this.timeSlots.add(new TimeSlot("4", "3:45 p.m.", "5:15 p.m."));
        this.timeSlots.add(new TimeSlot("5", "5:30 p.m.", "7:00 p.m."));
        this.timeSlots.add(new TimeSlot("6", "7:15 p.m.", "8:45 p.m."));
        this.timeSlots.add(new TimeSlot("7", "9:00 p.m.", "10:30 p.m."));
    }

    /**
     * Cancel a reservation
     * @param reservationId The reservation ID
     * @param userEmail The email of the user attempting to cancel
     * @return true if cancellation was successful, false otherwise
     * @throws IllegalArgumentException if the reservation doesn't exist
     * @throws SecurityException if the user is not authorized to cancel the reservation
     */
    public boolean cancelReservation(String reservationId, String userEmail) {
        LOG.info("Attempting to cancel reservation {} by user {}", reservationId, userEmail);

        // Get the reservation
        Map<String, AttributeValue> reservation = reservationDeletionRepository.getReservationById(reservationId);

        // Check if reservation exists
        if (reservation == null) {
            LOG.warn("Reservation not found: {}", reservationId);
            throw new IllegalArgumentException("Reservation not found");
        }

        // Check if the user is authorized to cancel
        String customerEmail = reservation.get("customerEmail").s();
        String waiterEmail = reservation.get("waiterEmail").s();

        if (!userEmail.equals(customerEmail) && !userEmail.equals(waiterEmail)) {
            LOG.warn("User {} not authorized to cancel reservation {}", userEmail, reservationId);
            throw new SecurityException("You are not authorized to cancel this reservation");
        }

        // Check if the reservation is already cancelled
        String currentStatus = reservation.get("status").s();
        if ("Cancelled".equals(currentStatus)) {
            LOG.info("Reservation {} is already cancelled", reservationId);
            return true;
        }

        // Getting reservation date and slot
        String reservationDate = reservation.get("date").s();
        String slotId = reservation.get("slotId").s();

        // Check if cancellation is at least 30 minutes before the reservation time
        if (!isValidCancellationTime(reservationDate, slotId)) {
            LOG.warn("Cancellation rejected: less than 30 minutes before reservation time");
            throw new UnprocessableException("Reservations can only be cancelled at least 30 minutes before the scheduled time");
        }

        // Cancel the reservation
        return reservationDeletionRepository.cancelReservation(reservationId,"Cancelled");
    }
    /**
     * Check if the current time is at least 30 minutes before the reservation time
     * @param reservationDate The reservation date in format "yyyy-MM-dd"
     * @param slotId The time slot ID
     * @return true if cancellation is allowed, false otherwise
     */
    private boolean isValidCancellationTime(String reservationDate, String slotId) {
        try {
            // Get current date and time
            ZoneId istZone = ZoneId.of("Asia/Kolkata");  // IST timezone
            ZonedDateTime istNow = ZonedDateTime.now(istZone);
            LocalDateTime now = istNow.toLocalDateTime();
            LOG.info("Current time: {}", now);

            // Parse reservation date
            LocalDate bookingDate = LocalDate.parse(reservationDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LOG.info("Booking date: {}", bookingDate);

            // If reservation is for a future date, cancellation is allowed
            if (bookingDate.isAfter(now.toLocalDate())) {
                LOG.info("Booking is for a future date, cancellation allowed");
                return true;
            }

            // If reservation is for a past date, cancellation is not allowed
            if (bookingDate.isBefore(now.toLocalDate())) {
                LOG.info("Booking is for a past date, cancellation not allowed");
                return false;
            }

            // Reservation is for today, check the time
            // Find the slot start time
            String slotStartTime = null;
            for (TimeSlot slot : timeSlots) {
                if (slot.getSlotId().equals(slotId)) {
                    slotStartTime = slot.getSlotStartTime();
                    break;
                }
            }

            if (slotStartTime == null) {
                LOG.error("Invalid slot ID: {}", slotId);
                return false;
            }

            LOG.info("Slot start time string: {}", slotStartTime);

            // Parse the slot start time
            LocalTime reservationTime = parseTimeString(slotStartTime);
            if (reservationTime == null) {
                LOG.error("Failed to parse reservation time");
                return false;
            }

            LOG.info("Parsed reservation time: {}", reservationTime);

            // Combine date and time to get the reservation datetime
            LocalDateTime reservationDateTime = LocalDateTime.of(bookingDate, reservationTime);
            LOG.info("Reservation datetime: {}", reservationDateTime);

            // Calculate the time difference in minutes
            long minutesUntilReservation = java.time.Duration.between(now, reservationDateTime).toMinutes();
            LOG.info("Minutes until reservation: {}", minutesUntilReservation);

            // Check if current time is at least 30 minutes before reservation time
            boolean isValid = minutesUntilReservation >= 30;
            LOG.info("Is valid cancellation time: {}", isValid);

            return isValid;

        } catch (DateTimeParseException e) {
            LOG.error("Error parsing date/time: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            LOG.error("Error checking cancellation time: {}", e.getMessage());
            return false;
        }
    }
    /**
     * Parse time string in format like "10:30 a.m." to LocalTime
     * @param timeString The time string to parse
     * @return LocalTime object or null if parsing fails
     */
    private LocalTime parseTimeString(String timeString) {
        try {
            // Handle different time formats
            if (timeString.contains("a.m.") || timeString.contains("p.m.")) {
                // Convert from "10:30 a.m." format
                String[] parts = timeString.split(" ");
                String timePart = parts[0];
                boolean isPM = parts[1].equals("p.m.");

                String[] hourMinute = timePart.split(":");
                int hour = Integer.parseInt(hourMinute[0]);
                int minute = Integer.parseInt(hourMinute[1]);

                if (isPM && hour < 12) {
                    hour += 12;
                } else if (!isPM && hour == 12) {
                    hour = 0;
                }

                return LocalTime.of(hour, minute);
            } else {
                // Already in 24-hour format like "12:15"
                return LocalTime.parse(timeString);
            }
        } catch (Exception e) {
            LOG.error("Error parsing time string: {}", timeString, e);
            return null;
        }
    }
}