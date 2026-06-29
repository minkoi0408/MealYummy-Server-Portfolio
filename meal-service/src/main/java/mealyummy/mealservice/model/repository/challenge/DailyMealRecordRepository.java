package mealyummy.mealservice.model.repository.challenge;

import mealyummy.mealservice.model.entity.challenge.DailyMealRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DailyMealRecordRepository extends MongoRepository<DailyMealRecord, String> {
    Optional<DailyMealRecord> findByUserIdAndRecordDate(String userId, String recordDate);
    List<DailyMealRecord> findByUserIdAndRecordDateStartingWith(String userId, String yearMonth);
}
