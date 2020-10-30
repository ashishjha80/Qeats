/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

//import ch.hsr.geohash.GeoHash;
import com.crio.qeats.dto.Restaurant;
//import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
//import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashSet;
import java.util.List;
//import java.util.Optional;
//import java.util.Set;
//import java.util.concurrent.Future;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;


@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {




  /*@Autowired
  private MongoTemplate mongoTemplate;*/
  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {
    
    List<RestaurantEntity> allRestaurants = restaurantRepository.findAll();
    List<Restaurant> restaurants = new ArrayList<>();
    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity currentRes : allRestaurants) {
      if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, longitude, 
              servingRadiusInKms)) {
        
        Restaurant res = modelMapper.map(currentRes, Restaurant.class);
        restaurants.add(res);
      }
      
    }
    
    //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
    return restaurants;
  }








  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      Double dist = GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude());
      
      return dist < servingRadiusInKms;
    }

    return false;
  }



}

