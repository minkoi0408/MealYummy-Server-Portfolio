package mealyummy.mealservice.model.repository.challenge;

import mealyummy.mealservice.model.entity.challenge.UserStreak;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserStreakRepository extends MongoRepository<UserStreak, String> {
    Optional<UserStreak> findByUserId(String userId);
}
