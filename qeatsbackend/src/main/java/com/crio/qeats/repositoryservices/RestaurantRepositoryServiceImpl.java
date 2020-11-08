/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
//import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
//import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
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

import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;

@Service
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  /*
   * @Autowired private MongoTemplate mongoTemplate;
   */
  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

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

  // CHECKSTYLE:OFF
  // CHECKSTYLE:ON

  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude, 
      LocalTime currentTime,Double servingRadiusInKms) throws IOException {

    List<Restaurant> restaurants = new ArrayList<>();
    // TODO: CRIO_TASK_MODULE_REDIS
    // We want to use cache to speed things up. Write methods that perform the same functionality,
    // but using the cache if it is present and reachable.
    // Remember, you must ensure that if cache is not present, the queries are directed at the
    // database instead.


    //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
    List<RestaurantEntity> allRestaurants = null;
    GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 7);
    try {
      if (redisConfiguration.isCacheAvailable()) {
        Jedis jedis = redisConfiguration.getJedisPool().getResource();
        ObjectMapper objectMapper = new ObjectMapper();
  
        if (jedis.get(geoHash.toBase32()) == null) {
          allRestaurants = restaurantRepository.findAll();
          jedis.set(geoHash.toBase32(),objectMapper.writeValueAsString(allRestaurants));
        } else {
          String res = jedis.get(geoHash.toBase32());
          RestaurantEntity[] cachedRes = objectMapper.readValue(res, RestaurantEntity[].class);
          allRestaurants = Arrays.asList(cachedRes);
        }
        
      } else {
        allRestaurants = restaurantRepository.findAll();
        
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  

    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity currentRes : allRestaurants) {
      if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, longitude, 
              servingRadiusInKms)) {
      
        Restaurant res = modelMapper.map(currentRes, Restaurant.class);
        restaurants.add(res);
      }    
    }

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
      return GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude())
          < servingRadiusInKms;
    }

    return false;
  }



}

