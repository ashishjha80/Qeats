/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositories;

import com.crio.qeats.models.RestaurantEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface RestaurantRepository extends MongoRepository<RestaurantEntity, String> {

  RestaurantEntity findByRestaurantId(String restaurantId);

  @Query("{'name' : {$regex : '^?0$', $options: 'i'} }")
  Optional<List<RestaurantEntity>> findRestaurantsByNameExact(String resName);

  @Query("{'name' : {$regex : ?0, $options: 'i'}}")
  Optional<List<RestaurantEntity>> findRestaurantsByNamePartial(String resName);

  @Query("{'attributes' : {$regex : ?0 , $options: 'i'}}")
  Optional<List<RestaurantEntity>> findRestaurantsByAttributes(String attributeName);

  Optional<List<RestaurantEntity>> findRestaurantsByRestaurantIdIn(List<String> restaurantIds);
}

