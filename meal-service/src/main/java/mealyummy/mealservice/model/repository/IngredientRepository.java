package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.Ingredient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IngredientRepository extends MongoRepository<Ingredient, String> {
    List<Ingredient> findAllByActiveTrue();
    boolean existsByNameAndActiveTrue(String name);
    List<Ingredient> findByNameInAndActiveTrue(List<String> names);
}
