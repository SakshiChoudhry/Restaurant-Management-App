package com.restaurantapp.Repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.restaurantapp.Model.Reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TableAvailabilityRepository {
    private static final Logger LOG = LoggerFactory.getLogger(TableAvailabilityRepository.class);

    private final DynamoDB dynamoDB;
    private final String tableName;

    public TableAvailabilityRepository() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
        this.tableName = System.getenv("booking_table");
        LOG.info("Initialized TableAvailabilityRepository with table name: {}", tableName);
    }

    public List<Reservation> findByDate(LocalDate date) {
        LOG.info("Finding reservations for date: {}", date);
        List<Reservation> reservations = new ArrayList<>();

        try {
            if (tableName == null || tableName.isEmpty()) {
                LOG.warn("Reservation table name is null or empty. Check RESERVATIONS_TABLE_NAME environment variable.");
                return reservations;
            }

            com.amazonaws.services.dynamodbv2.document.Table ddbTable = dynamoDB.getTable(tableName);

            // Use expression attribute names to handle reserved keyword 'date'
            Map<String, String> nameMap = new HashMap<>();
            nameMap.put("#dateAttr", "date");

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(":dateValue", date.toString());

            ScanSpec scanSpec = new ScanSpec()
                    .withFilterExpression("#dateAttr = :dateValue")
                    .withNameMap(nameMap)
                    .withValueMap(valueMap);

            ItemCollection<ScanOutcome> items = ddbTable.scan(scanSpec);
            Iterator<Item> iterator = items.iterator();

            while (iterator.hasNext()) {
                Item item = iterator.next();
                LOG.debug("Raw reservation data: {}", item.toJSON());
                reservations.add(mapToReservation(item));
            }

            LOG.info("Found {} reservations for date {}", reservations.size(), date);
        } catch (Exception e) {
            LOG.error("Error fetching reservations by date from DynamoDB: {}", e.getMessage(), e);
            // Return empty list instead of throwing exception to be more resilient
            return new ArrayList<>();
        }

        return reservations;
    }


    private Reservation mapToReservation(Item item) {
        Reservation reservation = new Reservation();

        try {
            // Map fields based on the updated schema
            if (item.hasAttribute("reservationId")) {
                reservation.setReservationId(item.getString("reservationId"));
            } else if (item.hasAttribute("id")) {
                // Fallback to "id" if "reservationId" doesn't exist
                reservation.setReservationId(item.getString("id"));
            }

            if (item.hasAttribute("customerEmail")) {
                reservation.setCustomerEmail(item.getString("customerEmail"));
            }

            if (item.hasAttribute("date")) {
                reservation.setDate(item.getString("date"));
            }

            if (item.hasAttribute("locationId")) {
                reservation.setLocationId(item.getString("locationId"));
            }

            if (item.hasAttribute("numberOfGuests")) {
                try {
                    reservation.setNumberOfGuests(item.getInt("numberOfGuests"));
                } catch (Exception e) {
                    // Try parsing as string
                    String guestsStr = item.getString("numberOfGuests");
                    reservation.setNumberOfGuests(Integer.parseInt(guestsStr));
                }
            }

            if (item.hasAttribute("slotId")) {
                reservation.setSlotId(item.getString("slotId"));
                // We don't need to set time from slotId here as TableService handles this conversion
            }

            if (item.hasAttribute("status")) {
                reservation.setStatus(item.getString("status"));
            }

            if (item.hasAttribute("tableId")) {
                reservation.setTableId(item.getString("tableId"));
            }

            if (item.hasAttribute("waiterEmail")) {
                reservation.setWaiterEmail(item.getString("waiterEmail"));
            }

            // Log the mapped reservation for debugging
            LOG.debug("Mapped reservation: id={}, tableId={}, slotId={}, status={}",
                    reservation.getReservationId(), reservation.getTableId(),
                    reservation.getSlotId(), reservation.getStatus());
        } catch (Exception e) {
            LOG.error("Error mapping reservation item: {}", e.getMessage(), e);
            // Return partially populated reservation object rather than failing completely
        }

        return reservation;
    }
}