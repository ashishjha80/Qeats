/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
//import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
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
  private ItemRepository itemRepository;

  @Autowired
  private MenuRepository menuRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

  /*
   * @Autowired private MongoTemplate mongoTemplate;
   */

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

  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, 
      Double longitude, LocalTime currentTime,
      Double servingRadiusInKms) {

    List<Restaurant> restaurants = new ArrayList<>();
    // TODO: CRIO_TASK_MODULE_REDIS
    // We want to use cache to speed things up. Write methods that perform the same
    // functionality,
    // but using the cache if it is present and reachable.
    // Remember, you must ensure that if cache is not present, the queries are
    // directed at the
    // database instead.

    // CHECKSTYLE:OFF
    // CHECKSTYLE:ON

    try {
      List<RestaurantEntity> allRestaurants = null;
      GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 7);
      if (redisConfiguration.isCacheAvailable()) {
        Jedis jedis = redisConfiguration.getJedisPool().getResource();
        ObjectMapper objectMapper = new ObjectMapper();

        if (jedis.get(geoHash.toBase32()) == null) {
          allRestaurants = restaurantRepository.findAll();
          jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS,
              objectMapper.writeValueAsString(allRestaurants));
        } else {
          String res = jedis.get(geoHash.toBase32());
          RestaurantEntity[] cachedRes = objectMapper.readValue(res, RestaurantEntity[].class);
          allRestaurants = Arrays.asList(cachedRes);
        }

      } else {
        allRestaurants = restaurantRepository.findAll();
      }

      ModelMapper modelMapper = modelMapperProvider.get();
      for (RestaurantEntity currentRes : allRestaurants) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {

          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          restaurants.add(res);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return restaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search
  // query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, 
      Double longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurants = new ArrayList<>();
    Set<String> setOfRes = new HashSet<>();
    Optional<List<RestaurantEntity>> exactNamesRes = restaurantRepository
        .findRestaurantsByNameExact(searchString);
    ModelMapper modelMapper = modelMapperProvider.get();
    if (exactNamesRes.isPresent()) {
      List<RestaurantEntity> exactRes = exactNamesRes.get();
      for (RestaurantEntity currentRes : exactRes) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {
          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          if (!setOfRes.contains(res.getRestaurantId())) {
            restaurants.add(res);
            setOfRes.add(res.getRestaurantId());
          }
        }
      }
    }

    Optional<List<RestaurantEntity>> partialNamesRes = restaurantRepository
        .findRestaurantsByNamePartial(searchString);
    if (partialNamesRes.isPresent()) {
      List<RestaurantEntity> partialRes = partialNamesRes.get();
      for (RestaurantEntity currentRes : partialRes) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {
          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          if (!setOfRes.contains(res.getRestaurantId())) {
            restaurants.add(res);
            setOfRes.add(res.getRestaurantId());
          }
        }
      }
    }

    return restaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, 
      Double longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants = new ArrayList<>();
    Optional<List<RestaurantEntity>> attributeNamesRes = restaurantRepository
        .findRestaurantsByAttributes(searchString);
    ModelMapper modelMapper = modelMapperProvider.get();
    if (attributeNamesRes.isPresent()) {
      List<RestaurantEntity> attributeRes = attributeNamesRes.get();
      for (RestaurantEntity currentRes : attributeRes) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {
          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          restaurants.add(res);
        }
      }
    }
    return restaurants;
  }

  private List<Restaurant> getRestaurantListServingItems(Double latitude, 
      Double longitude, LocalTime currentTime,
      Double servingRadiusInKms, List<ItemEntity> itemEntityList) {
    List<String> itemIdList = itemEntityList.stream().map(ItemEntity::getItemId)
        .collect(Collectors.toList());

    Optional<List<MenuEntity>> optionalMenuEntityList = menuRepository
        .findMenusByItemsItemIdIn(itemIdList);
    Optional<List<RestaurantEntity>> optionalRestaurantEntityList = Optional.empty();

    if (optionalMenuEntityList.isPresent()) {
      List<MenuEntity> menuEntityList = optionalMenuEntityList.get();
      List<String> restaurantIdList = menuEntityList.stream().map(MenuEntity::getRestaurantId)
          .collect(Collectors.toList());
      optionalRestaurantEntityList = restaurantRepository
          .findRestaurantsByRestaurantIdIn(restaurantIdList);
    }

    List<Restaurant> restaurantList = new ArrayList<>();
    ModelMapper modelMapper = modelMapperProvider.get();
    if (optionalRestaurantEntityList.isPresent()) {
      List<RestaurantEntity> restaurantEntityList = optionalRestaurantEntityList.get();

      List<RestaurantEntity> restaurantEntitiesFiltered = new ArrayList<>();

      for (RestaurantEntity restaurantEntity : restaurantEntityList) {
        if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, 
            latitude, longitude, servingRadiusInKms)) {
          restaurantEntitiesFiltered.add(restaurantEntity);
        }
      }

      restaurantList = restaurantEntitiesFiltered.stream()
          .map(restaurantEntity -> modelMapper.map(restaurantEntity, Restaurant.class))
              .collect(Collectors.toList());
    }

    return restaurantList;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or
  // partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, 
      Double longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {

    Optional<List<ItemEntity>> exactNamesItemEntity = itemRepository
        .findItemsByNameExact(searchString);

    Optional<List<ItemEntity>> partialNamesItemEntity = itemRepository
        .findItemsByNamePartial(searchString);

    List<ItemEntity> itemEntityList = exactNamesItemEntity.orElseGet(ArrayList::new);
    List<ItemEntity> inexactItemEntityList = partialNamesItemEntity.orElseGet(ArrayList::new);
    itemEntityList.addAll(inexactItemEntityList);

    return getRestaurantListServingItems(latitude, longitude, currentTime, 
        servingRadiusInKms, itemEntityList);

  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the
  // search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, 
      Double longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {

    Optional<List<ItemEntity>> attributeNamesItemEntity = itemRepository
        .findItemsByAttributes(searchString);

    List<ItemEntity> itemEntityList = attributeNamesItemEntity.orElseGet(ArrayList::new);
    return getRestaurantListServingItems(latitude, longitude, currentTime, 
        servingRadiusInKms, itemEntityList);
  }

  /**
   * Utility method to check if a restaurant is within the serving radius at a
   * given time.
   * 
   * @return boolean True if restaurant falls within serving radius and is open,
   *         false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity, 
      LocalTime currentTime, Double latitude,
      Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude, restaurantEntity.getLatitude(),
          restaurantEntity.getLongitude()) < servingRadiusInKms;
    }

    return false;
  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByNameMt(Double latitude, Double 
      longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    List<Restaurant> restaurants = new ArrayList<>();
    Set<String> setOfRes = new HashSet<>();
    Optional<List<RestaurantEntity>> exactNamesRes = restaurantRepository
        .findRestaurantsByNameExact(searchString);
    ModelMapper modelMapper = modelMapperProvider.get();
    if (exactNamesRes.isPresent()) {
      List<RestaurantEntity> exactRes = exactNamesRes.get();
      for (RestaurantEntity currentRes : exactRes) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {
          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          if (!setOfRes.contains(res.getRestaurantId())) {
            restaurants.add(res);
            setOfRes.add(res.getRestaurantId());
          }
        }
      }
    }

    Optional<List<RestaurantEntity>> partialNamesRes = restaurantRepository
        .findRestaurantsByNamePartial(searchString);
    if (partialNamesRes.isPresent()) {
      List<RestaurantEntity> partialRes = partialNamesRes.get();
      for (RestaurantEntity currentRes : partialRes) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {
          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          if (!setOfRes.contains(res.getRestaurantId())) {
            restaurants.add(res);
            setOfRes.add(res.getRestaurantId());
          }
        }
      }
    }

    return new AsyncResult<List<Restaurant>>(restaurants);
  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByAttributesMt(Double latitude, 
      Double longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    List<Restaurant> restaurants = new ArrayList<>();
    Optional<List<RestaurantEntity>> attributeNamesRes = restaurantRepository
        .findRestaurantsByAttributes(searchString);
    ModelMapper modelMapper = modelMapperProvider.get();
    if (attributeNamesRes.isPresent()) {
      List<RestaurantEntity> attributeRes = attributeNamesRes.get();
      for (RestaurantEntity currentRes : attributeRes) {
        if (isRestaurantCloseByAndOpen(currentRes, currentTime, latitude, 
            longitude, servingRadiusInKms)) {
          Restaurant res = modelMapper.map(currentRes, Restaurant.class);
          restaurants.add(res);
        }
      }
    }
    return new AsyncResult<List<Restaurant>>(restaurants);
  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByItemNameMt(Double latitude, 
      Double longitude, String searchString,
      LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    Optional<List<ItemEntity>> exactNamesItemEntity = itemRepository
        .findItemsByNameExact(searchString);

    Optional<List<ItemEntity>> partialNamesItemEntity = itemRepository
        .findItemsByNamePartial(searchString);

    List<ItemEntity> itemEntityList = exactNamesItemEntity.orElseGet(ArrayList::new);
    List<ItemEntity> inexactItemEntityList = partialNamesItemEntity.orElseGet(ArrayList::new);
    itemEntityList.addAll(inexactItemEntityList);

    return new AsyncResult<List<Restaurant>>(getRestaurantListServingItems(latitude, 
        longitude, currentTime, 
        servingRadiusInKms, itemEntityList));
  }

  @Override
  public Future<List<Restaurant>> findRestaurantsByItemAttributesMt(Double latitude, 
      Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    // TODO Auto-generated method stub
    Optional<List<ItemEntity>> attributeNamesItemEntity = itemRepository
        .findItemsByAttributes(searchString);

    List<ItemEntity> itemEntityList = attributeNamesItemEntity.orElseGet(ArrayList::new);
    return new AsyncResult<List<Restaurant>>(getRestaurantListServingItems(latitude, 
        longitude, currentTime, 
        servingRadiusInKms, itemEntityList));
  }



}

