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
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.restaurantapp.Model.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TableRepository {

    private final DynamoDB dynamoDB;
    private final String tableName;

    public TableRepository() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
        this.tableName = System.getenv("tables_table_name");
    }

    public List<Table> findAll() {
        List<Table> tables = new ArrayList<>();

        try {
            com.amazonaws.services.dynamodbv2.document.Table ddbTable = dynamoDB.getTable(tableName);
            ScanSpec scanSpec = new ScanSpec();

            ItemCollection<ScanOutcome> items = ddbTable.scan(scanSpec);
            Iterator<Item> iterator = items.iterator();

            while (iterator.hasNext()) {
                Item item = iterator.next();
                tables.add(mapToTable(item));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching tables from DynamoDB", e);
        }

        return tables;
    }

    public List<Table> findByLocationId(String locationId) {
        List<Table> tables = new ArrayList<>();

        try {
            com.amazonaws.services.dynamodbv2.document.Table ddbTable = dynamoDB.getTable(tableName);

            Map<String, String> nameMap = new HashMap<>();
            nameMap.put("#locationId", "locationId");

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put(":locationId", locationId);

            QuerySpec querySpec = new QuerySpec()
                    .withKeyConditionExpression("#locationId = :locationId")
                    .withNameMap(new NameMap().with("#locationId", "locationId"))
                    .withValueMap(new ValueMap().with(":locationId", locationId));

            ItemCollection<QueryOutcome> items = ddbTable.query(querySpec);
            Iterator<Item> iterator = items.iterator();

            while (iterator.hasNext()) {
                Item item = iterator.next();
                tables.add(mapToTable(item));
            }
        } catch (Exception e) {
            // If the table doesn't use locationId as the partition key, fall back to scan with filter
            return findAllByLocationId(locationId);
        }

        return tables;
    }

    private List<Table> findAllByLocationId(String locationId) {
        List<Table> tables = new ArrayList<>();

        try {
            com.amazonaws.services.dynamodbv2.document.Table ddbTable = dynamoDB.getTable(tableName);

            ScanSpec scanSpec = new ScanSpec()
                    .withFilterExpression("locationId = :locationId")
                    .withValueMap(new ValueMap().with(":locationId", locationId));

            ItemCollection<ScanOutcome> items = ddbTable.scan(scanSpec);
            Iterator<Item> iterator = items.iterator();

            while (iterator.hasNext()) {
                Item item = iterator.next();
                tables.add(mapToTable(item));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching tables by locationId from DynamoDB", e);
        }

        return tables;
    }

    private Table mapToTable(Item item) {
        Table table = new Table();
        table.setId(item.getString("tableId"));
        table.setLocationId(item.getString("locationId"));
        table.setTableNumber(item.getString("tableNumber"));
        table.setCapacity(item.getInt("capacity"));
        return table;
    }
}