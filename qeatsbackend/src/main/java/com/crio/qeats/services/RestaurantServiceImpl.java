
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;

import java.time.LocalTime;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Map;
import java.util.Set;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;

  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(GetRestaurantsRequest 
      getRestaurantsRequest, LocalTime currentTime) {
    
    log.info("findAllRestaurantsCloseBy called with {}",getRestaurantsRequest,currentTime);
    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    Double servingRadiusInKms;
    if ((currentTime.isAfter(LocalTime.of(7,59)) && currentTime.isBefore(LocalTime.of(10, 1)))
            || (currentTime.isAfter(LocalTime.of(12,59))
            && currentTime.isBefore(LocalTime.of(14, 1)))
            || (currentTime.isAfter(LocalTime.of(18,59)) 
            && currentTime.isBefore(LocalTime.of(21, 1)))) {
      servingRadiusInKms = peakHoursServingRadiusInKms;
    } else {
      servingRadiusInKms = normalHoursServingRadiusInKms;
    }
    long startTimeInMillis = System.currentTimeMillis();

    List<Restaurant> nearRestaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(
            latitude, longitude, currentTime, servingRadiusInKms);
    log.info("findAllRestaurantsCloseBy returned with {}",nearRestaurants);
    long endTimeInMillis = System.currentTimeMillis();
    System.out.println("Your data layer took :" + (endTimeInMillis - startTimeInMillis));
    return new GetRestaurantsResponse(nearRestaurants);
  }



  


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    List<Restaurant> restaurants = new ArrayList<>();   
    if (getRestaurantsRequest.getSearchFor().length() == 0) {
      return new GetRestaurantsResponse(restaurants);
    }
    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    String searchString = getRestaurantsRequest.getSearchFor();
    Double servingRadiusInKms;
    if ((currentTime.isAfter(LocalTime.of(7,59)) && currentTime.isBefore(LocalTime.of(10, 1)))
            || (currentTime.isAfter(LocalTime.of(12,59))
            && currentTime.isBefore(LocalTime.of(14, 1)))
            || (currentTime.isAfter(LocalTime.of(18,59)) 
            && currentTime.isBefore(LocalTime.of(21, 1)))) {
      servingRadiusInKms = peakHoursServingRadiusInKms;
    } else {
      servingRadiusInKms = normalHoursServingRadiusInKms;
    }
    Set<Restaurant> setOfRes =  new HashSet<>();
    //ArrayList<ArrayList<Restaurant>> allRes = new ArrayList<ArrayList<Restaurant>>();
    List<Restaurant> resByName = restaurantRepositoryService
            .findRestaurantsByName(latitude, longitude, searchString,
            currentTime, servingRadiusInKms);
    for (Restaurant res : resByName) {
      if (!setOfRes.contains(res)) {
        restaurants.add(res);
        setOfRes.add(res);
      }
        
    }
    //allRes.add((ArrayList<Restaurant>) resByName);
    List<Restaurant> resByResAttribute = restaurantRepositoryService
            .findRestaurantsByAttributes(latitude, longitude, searchString,
            currentTime, servingRadiusInKms);
    for (Restaurant res : resByResAttribute) {
      if (!setOfRes.contains(res)) {
        restaurants.add(res);
        setOfRes.add(res);
      }
        
    }
    //allRes.add((ArrayList<Restaurant>) resByResAttribute);
    List<Restaurant> resByItemName = restaurantRepositoryService
            .findRestaurantsByItemName(latitude, longitude, searchString,
            currentTime, servingRadiusInKms);
    for (Restaurant res : resByItemName) {
      if (!setOfRes.contains(res)) {
        restaurants.add(res);
        setOfRes.add(res);
      }
        
    }
    //allRes.add((ArrayList<Restaurant>) resByItemName);
    List<Restaurant> resByItemAttribute = restaurantRepositoryService
            .findRestaurantsByItemAttributes(latitude, longitude, searchString,
            currentTime, servingRadiusInKms);
    //allRes.add((ArrayList<Restaurant>) resByItemAttribute);
    
    for (Restaurant res : resByItemAttribute) {
      if (!setOfRes.contains(res)) {
        restaurants.add(res);
        setOfRes.add(res);
      }
        
    }
    
    
    return new GetRestaurantsResponse(restaurants);
  }

}

