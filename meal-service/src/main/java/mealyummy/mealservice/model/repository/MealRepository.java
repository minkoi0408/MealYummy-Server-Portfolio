package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.Meal;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealRepository extends MongoRepository<Meal,String> {
    boolean existsByName(String formattedName);
}
