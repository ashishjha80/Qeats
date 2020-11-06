
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
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
import java.util.List;
//import java.util.Map;
//import java.util.Set;
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
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    
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


}

