package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.Ingredient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngredientRepository extends MongoRepository<Ingredient, String> {
}
