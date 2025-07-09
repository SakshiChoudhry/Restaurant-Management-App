package com.restaurantapp.Repository;

import com.amazonaws.services.dynamodbv2.xspec.S;
import com.restaurantapp.Model.Location;
import com.restaurantapp.Model.LocationDTO;
import com.restaurantapp.Model.SpecialityDishes;
import org.checkerframework.checker.units.qual.A;
import org.w3c.dom.Attr;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LocationRepository {
    private final DynamoDbClient dynamoDbClient;
    private final String restaurantTable=System.getenv("location_table");
    private final String dishesTable=System.getenv("dishes_table");

    private static final Logger LOG = Logger.getLogger(LocationRepository.class.getName());
    public LocationRepository(){
        this.dynamoDbClient=DynamoDbClient.builder()
                .region(Region.AP_SOUTHEAST_1)
                .build();
    }

    public List<Location> findAll(){
        List<Location> locations=new ArrayList<>();
        ScanRequest scanRequest=ScanRequest.builder()
                .tableName(restaurantTable)
                .build();


        ScanResponse response=dynamoDbClient.scan(scanRequest);
        for(Map<String, AttributeValue> item:response.items()){
            locations.add(mapItemToLocation(item));
        }
        return locations;
    }
    public Location mapItemToLocation(Map<String, AttributeValue> item){
        return new Location(
                item.get("locationId").s(),
                item.get("address").s(),
                item.get("description").s(),
                item.get("totalCapacity").s(),
                item.get("averageOccupancy").s(),
                item.get("imageUrl").s(),
                item.get("rating").s()
        );
    }
    public List<SpecialityDishes> findSpecialityDishes(String locationId){
        List<SpecialityDishes> specialityDishes=new ArrayList<>();
        Map<String,AttributeValue> key=new HashMap<>();
        key.put("locationId", AttributeValue.builder().s(locationId).build());
        GetItemRequest getItemRequest=GetItemRequest.builder()
                .tableName(restaurantTable)
                .key(key)
                .build();
        GetItemResponse getItemResponse=dynamoDbClient.getItem(getItemRequest);
        if(getItemResponse.hasItem() && getItemResponse.item().containsKey("listOfDishes")){
            LOG.info("List of dishes found");
            List<String> dishIds=new ArrayList<>();
            List<AttributeValue> dishIdValue=getItemResponse.item().get("listOfDishes").l();
            LOG.info(dishIdValue.toString());
            for(AttributeValue dishId:dishIdValue){
                dishIds.add(dishId.s());
            }
            LOG.info(dishIds.toString());
            for(String dishId:dishIds){
                Map<String, AttributeValue> dishKey = new HashMap<>();
                LOG.info("Dish ID: "+dishId);
                dishKey.put("dishId", AttributeValue.builder().s(dishId).build());
                GetItemRequest dishRequest = GetItemRequest.builder()
                        .tableName(dishesTable)
                        .key(dishKey)
                        .build();

                GetItemResponse dishResponse = dynamoDbClient.getItem(dishRequest);
                if (dishResponse.hasItem()) {
                    specialityDishes.add(mapItemToSpecialityDishes(dishResponse.item()));
                }
            }
        }

        return specialityDishes;
    }
    public SpecialityDishes mapItemToSpecialityDishes(Map<String, AttributeValue> item){
        SpecialityDishes specialityDishes=new SpecialityDishes();
        specialityDishes.setName(item.get("dishName").s());
        specialityDishes.setWeight(item.get("weight").s());
        specialityDishes.setImageUrl(item.get("imageUrl").s());
        specialityDishes.setPrice(item.get("price").s());

        return specialityDishes;
    }
    public Optional<Location> findLocationById(String locationId) {
        Map<String,AttributeValue> hs =new HashMap<>();
        hs.put("locationId",AttributeValue.builder().s(locationId).build());

        GetItemRequest getItemRequest = GetItemRequest.builder()
                .tableName(restaurantTable)
                .key(hs)
                .build();

        try {
            GetItemResponse response = dynamoDbClient.getItem(getItemRequest);
            if (response.hasItem()) {
                return Optional.of(mapItemToLocation(response.item()));
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<LocationDTO> getAllLocationsBySakshi() {
        ScanRequest scanRequest = ScanRequest.builder().tableName(restaurantTable).build();
        ScanResponse response = dynamoDbClient.scan(scanRequest);

        return response.items().stream()
                .map(item -> new LocationDTO(
                        item.get("locationId").s(),
                        item.get("address").s()
                ))
                .collect(Collectors.toList());
    }

}
