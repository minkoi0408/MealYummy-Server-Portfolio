package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.food.FavoriteMeal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteMealRepository extends MongoRepository<FavoriteMeal, String> {
    Optional<FavoriteMeal> findByUserIdAndMealId(String userId, String mealId);
    Page<FavoriteMeal> findByUserId(String userId, Pageable pageable);
    
    @Query(value = "{ 'userId' : ?0 }", fields = "{ 'mealId' : 1 }")
    List<FavoriteMeal> findMealIdsByUserId(String userId);
    
    void deleteByUserIdAndMealId(String userId, String mealId);
}
