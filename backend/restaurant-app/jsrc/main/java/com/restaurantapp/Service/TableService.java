package com.restaurantapp.Service;

import com.restaurantapp.Model.AvailableTable;
import com.restaurantapp.Model.Reservation;
import com.restaurantapp.Model.Location;
import com.restaurantapp.Model.Table;
import com.restaurantapp.Repository.TableAvailabilityRepository;
import com.restaurantapp.Repository.RestaurantLocationRepository;
import com.restaurantapp.Repository.TableRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class TableService {
    private static final Logger LOG = LoggerFactory.getLogger(TableService.class);

    private final TableRepository tableRepository;
    private final TableAvailabilityRepository tableAvailabilityRepository;
    private final RestaurantLocationRepository restaurantLocationRepository;

    // Define time slots with their display formats
    private static final Map<String, String> TIME_SLOTS = new LinkedHashMap<>();
    static {
        TIME_SLOTS.put("10:30", "10:30 a.m. - 12:00 p.m.");
        TIME_SLOTS.put("12:15", "12:15 p.m. - 1:45 p.m.");
        TIME_SLOTS.put("14:00", "2:00 p.m. - 3:30 p.m.");
        TIME_SLOTS.put("15:45", "3:45 p.m. - 5:15 p.m.");
        TIME_SLOTS.put("17:30", "5:30 p.m. - 7:00 p.m.");
        TIME_SLOTS.put("19:15", "7:15 p.m. - 8:45 p.m.");
        TIME_SLOTS.put("21:00", "9:00 p.m. - 10:30 p.m.");
    }

    private static final Map<String, String> SLOT_ID_TO_TIME = new HashMap<>();
    static {
        SLOT_ID_TO_TIME.put("1", "10:30");
        SLOT_ID_TO_TIME.put("2", "12:15");
        SLOT_ID_TO_TIME.put("3", "14:00");
        SLOT_ID_TO_TIME.put("4", "15:45");
        SLOT_ID_TO_TIME.put("5", "17:30");
        SLOT_ID_TO_TIME.put("6", "19:15");
        SLOT_ID_TO_TIME.put("7", "21:00");
    }

    // Map time values to slot IDs (reverse of SLOT_ID_TO_TIME)
    private static final Map<String, String> TIME_TO_SLOT_ID = new HashMap<>();
    static {
        for (Map.Entry<String, String> entry : SLOT_ID_TO_TIME.entrySet()) {
            TIME_TO_SLOT_ID.put(entry.getValue(), entry.getKey());
        }
    }

    // Default time slots for reservations (just the keys from TIME_SLOTS)
    private static final List<String> DEFAULT_TIME_SLOTS = new ArrayList<>(TIME_SLOTS.keySet());

    private static final int RESERVATION_DURATION_MINUTES = 90; // 1.5 hours
    private static final int RESERVATION_BUFFER_MINUTES = 15;

    public TableService() {
        this.tableRepository = new TableRepository();
        this.tableAvailabilityRepository = new TableAvailabilityRepository();
        this.restaurantLocationRepository = new RestaurantLocationRepository();
    }

    public List<AvailableTable> findAvailableTables(String locationId, String dateStr, String timeStr, Integer guests) {
        try {
            LOG.info("Finding available tables with params: locationId={}, date={}, time={}, guests={}",
                    locationId, dateStr, timeStr, guests);

            List<AvailableTable> availableTables = new ArrayList<>();

            LocalDate date = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    date = LocalDate.parse(dateStr);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD");
                }
            }

            LocalTime time = null;
            List<String> nearestTimeSlots = null;
            if (timeStr != null && !timeStr.isEmpty()) {
                try {
                    time = LocalTime.parse(timeStr);
                    nearestTimeSlots = findNearestTimeSlots(time);
                    LOG.info("Found nearest time slots: {}", nearestTimeSlots);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid time format. Please use HH:MM");
                }
            }

            List<Table> tables;
            if (locationId != null && !locationId.isEmpty()) {
                tables = tableRepository.findByLocationId(locationId);
            } else {
                tables = tableRepository.findAll();
            }
            LOG.info("Found {} tables matching location criteria", tables.size());

            // Log all tables for debugging
            for (Table table : tables) {
                LOG.debug("Table: id={}, locationId={}, tableNumber={}, capacity={}",
                        table.getId(), table.getLocationId(), table.getTableNumber(), table.getCapacity());
            }

            if (guests != null) {
                tables = tables.stream()
                        .filter(table -> table.getCapacity() >= guests)
                        .collect(Collectors.toList());
                LOG.info("After capacity filtering: {} tables remain", tables.size());
            }

            List<Reservation> reservations = new ArrayList<>();
            if (date != null) {
                try {
                    reservations = tableAvailabilityRepository.findByDate(date);
                    LOG.info("Found {} reservations for date {}", reservations.size(), date);

                    // Log each reservation for debugging
                    for (Reservation res : reservations) {
                        LOG.debug("Reservation: id={}, tableId={}, slotId={}, status={}",
                                res.getReservationId(), res.getTableId(), res.getSlotId(), res.getStatus());
                    }
                } catch (Exception e) {
                    LOG.error("Error fetching reservations for date {}: {}", date, e.getMessage(), e);
                    // Continue with empty reservations list
                }
            }

            Map<String, Location> locationMap = new HashMap<>();
            try {
                restaurantLocationRepository.findAll().forEach(location -> locationMap.put(location.getLocationId(), location));
                LOG.info("Found {} locations", locationMap.size());
            } catch (Exception e) {
                LOG.error("Error fetching locations: {}", e.getMessage(), e);
                // Continue with empty location map
            }

            for (Table table : tables) {
                try {
                    LOG.debug("Processing table: id={}, locationId={}, tableNumber={}, capacity={}",
                            table.getId(), table.getLocationId(), table.getTableNumber(), table.getCapacity());

                    Location location = locationMap.get(table.getLocationId());
                    if (location == null) {
                        LOG.warn("No location found for table {} with locationId {}", table.getId(), table.getLocationId());
                        continue;
                    }

                    List<String> availableSlots = calculateAvailableTimeSlots(table, date, time, nearestTimeSlots, reservations);
                    LOG.info("Table {} has {} available slots: {}", table.getId(), availableSlots.size(), availableSlots);

                    if (!availableSlots.isEmpty()) {
                        AvailableTable availableTable = new AvailableTable();
                        availableTable.setLocationId(table.getLocationId());
                        availableTable.setLocationAddress(location.getLocationAddress());
                        availableTable.setTableNumber(table.getTableNumber());
                        availableTable.setCapacity(String.valueOf(table.getCapacity()));
                        availableTable.setAvailableSlots(availableSlots);

                        availableTables.add(availableTable);
                    }
                } catch (Exception e) {
                    LOG.error("Error processing table {}: {}", table.getId(), e.getMessage(), e);
                    // Continue with next table
                }
            }

            return availableTables;
        } catch (Exception e) {
            LOG.error("Unexpected error in findAvailableTables: {}", e.getMessage(), e);
            throw e; // Re-throw to be handled by the controller
        }
    }

    /**
     * Find the nearest 2 time slots to the requested time
     * @param requestedTime The time requested by the user
     * @return List of the nearest 2 time slots in 24-hour format
     */
    private List<String> findNearestTimeSlots(LocalTime requestedTime) {
        try {
            // Check if the requested time exactly matches one of our slots
            String requestedTimeStr = requestedTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (TIME_SLOTS.containsKey(requestedTimeStr)) {
                return Collections.singletonList(requestedTimeStr);
            }

            // Find the 2 nearest time slots
            List<String> result = new ArrayList<>(2);
            TreeMap<Long, String> distanceMap = new TreeMap<>();

            for (String slotTime : DEFAULT_TIME_SLOTS) {
                LocalTime slotLocalTime = LocalTime.parse(slotTime);
                long distance = Math.abs(slotLocalTime.toSecondOfDay() - requestedTime.toSecondOfDay());
                distanceMap.put(distance, slotTime);
            }

            // Get the 2 closest slots
            int count = 0;
            for (String slot : distanceMap.values()) {
                result.add(slot);
                count++;
                if (count == 2) break;
            }

            return result;
        } catch (Exception e) {
            LOG.error("Error finding nearest time slots: {}", e.getMessage(), e);
            // Return a safe default if there's an error
            return new ArrayList<>(DEFAULT_TIME_SLOTS.subList(0, Math.min(2, DEFAULT_TIME_SLOTS.size())));
        }
    }

    private List<String> calculateAvailableTimeSlots(Table table, LocalDate date, LocalTime requestedTime,
                                                     List<String> nearestTimeSlots, List<Reservation> reservations) {
        try {
            LOG.debug("Calculating available slots for table: id={}, locationId={}, tableNumber={}, capacity={}",
                    table.getId(), table.getLocationId(), table.getTableNumber(), table.getCapacity());

            // If no date is specified, return all time slots as available
            if (date == null) {
                if (requestedTime != null && nearestTimeSlots != null) {
                    // If only time is specified (no date), return only the nearest time slots
                    List<String> formattedSlots = new ArrayList<>();
                    for (String slot : nearestTimeSlots) {
                        String formattedSlot = TIME_SLOTS.get(slot);
                        if (formattedSlot != null) {
                            formattedSlots.add(formattedSlot);
                        }
                    }
                    return formattedSlots;
                }
                // If no time and no date, return all formatted slots
                return new ArrayList<>(TIME_SLOTS.values());
            }

            // Start with all time slots
            List<String> availableTimeKeys = new ArrayList<>(DEFAULT_TIME_SLOTS);

            // Filter reservations for this specific table on the given date
            List<Reservation> tableReservations = reservations.stream()
                    .filter(reservation -> {
                        try {
                            // Only consider active reservations (not cancelled)
                            if (reservation.getStatus() != null &&
                                    reservation.getStatus().equalsIgnoreCase("Cancelled")) {
                                LOG.debug("Skipping cancelled reservation: {}", reservation.getReservationId());
                                return false;
                            }

                            // Ensure both tableId values are treated as strings for comparison
                            String reservationTableId = reservation.getTableId();
                            String tableId = table.getId();

                            LOG.debug("Comparing reservation tableId '{}' with table.getId() '{}'",
                                    reservationTableId, tableId);

                            boolean matches = reservationTableId != null &&
                                    reservationTableId.equals(tableId);

                            if (matches) {
                                LOG.info("Found matching reservation: id={}, tableId={}, slotId={}",
                                        reservation.getReservationId(), reservation.getTableId(), reservation.getSlotId());
                            }

                            return matches;
                        } catch (Exception e) {
                            LOG.warn("Error comparing reservation tableId: {}", e.getMessage());
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            LOG.info("Found {} active reservations for table {} on date {}",
                    tableReservations.size(), table.getId(), date);

            // Check each reservation and remove corresponding time slots
            for (Reservation reservation : tableReservations) {
                try {
                    String slotId = reservation.getSlotId();
                    if (slotId != null && !slotId.isEmpty()) {
                        // Convert slotId to time value
                        String timeValue = SLOT_ID_TO_TIME.get(slotId);
                        if (timeValue != null) {
                            LOG.info("Removing time slot {} (slotId {}) from available slots for table {}",
                                    timeValue, slotId, table.getId());
                            availableTimeKeys.remove(timeValue);
                        } else {
                            LOG.warn("No time mapping found for slotId: {}", slotId);
                        }
                    } else {
                        // Fallback to using time directly if available
                        String timeValue = reservation.getTime();
                        if (timeValue != null && !timeValue.isEmpty()) {
                            LOG.info("Removing time slot {} from available slots for table {}",
                                    timeValue, table.getId());
                            availableTimeKeys.remove(timeValue);
                        } else {
                            LOG.warn("Reservation {} has neither slotId nor time",
                                    reservation.getReservationId());
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Error processing reservation {}: {}",
                            reservation.getReservationId(), e.getMessage());
                }
            }

            // If a specific time is requested, filter to show only the nearest time slots
            if (requestedTime != null && nearestTimeSlots != null) {
                // Filter to keep only the nearest time slots that are available
                List<String> availableNearestSlots = availableTimeKeys.stream()
                        .filter(nearestTimeSlots::contains)
                        .collect(Collectors.toList());

                LOG.debug("Available nearest slots for table {}: {}", table.getId(), availableNearestSlots);

                // If none of the nearest slots are available, return empty list
                if (availableNearestSlots.isEmpty()) {
                    return Collections.emptyList();
                }

                // Convert the available nearest slots to their formatted display versions
                List<String> formattedSlots = availableNearestSlots.stream()
                        .map(slot -> TIME_SLOTS.getOrDefault(slot, slot))
                        .collect(Collectors.toList());

                return formattedSlots;
            }

            // Convert all available time keys to their display formats
            List<String> formattedSlots = availableTimeKeys.stream()
                    .map(slot -> TIME_SLOTS.getOrDefault(slot, slot))
                    .collect(Collectors.toList());

            return formattedSlots;
        } catch (Exception e) {
            LOG.error("Error calculating available time slots: {}", e.getMessage(), e);
            // Return an empty list if there's an error
            return Collections.emptyList();
        }
    }

    /**
     * Helper method to convert a time string to a slot ID
     * @param timeStr Time string in HH:mm format
     * @return The corresponding slot ID or null if no match
     */
    public static String getSlotIdForTime(String timeStr) {
        return TIME_TO_SLOT_ID.get(timeStr);
    }

    /**
     * Helper method to convert a slot ID to a time string
     * @param slotId The slot ID
     * @return The corresponding time string in HH:mm format or null if no match
     */
    public static String getTimeForSlotId(String slotId) {
        return SLOT_ID_TO_TIME.get(slotId);
    }

    /**
     * Helper method to get the formatted display time for a slot ID
     * @param slotId The slot ID
     * @return The formatted display time (e.g., "2:00 p.m. - 3:30 p.m") or null if no match
     */
    public static String getFormattedTimeForSlotId(String slotId) {
        String timeStr = SLOT_ID_TO_TIME.get(slotId);
        if (timeStr != null) {
            return TIME_SLOTS.get(timeStr);
        }
        return null;
    }
}