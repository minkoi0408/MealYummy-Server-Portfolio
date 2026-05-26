package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.food.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findAllByParentIdAndActiveTrue(String parentId);
    boolean existsByIdAndActiveTrue(String id);
    boolean existsByName(String name);
    List<Category> findByNameIn(List<String> names);
    Optional<Category> findByName(String formattedName);
}