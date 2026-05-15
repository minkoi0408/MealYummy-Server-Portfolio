package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.MealPlanItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends MongoRepository<MealPlanItem, String> {
    List<MealPlanItem> findByUserIdOrderByDateDesc(String userId);
    List<MealPlanItem> findByUserIdAndDate(String userId, String date);
    void deleteByUserId(String userId);
}
