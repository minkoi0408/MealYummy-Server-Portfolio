package mealyummy.mealservice.model.repository;

import mealyummy.mealservice.model.entity.profile.UserMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMetricsRepository extends MongoRepository<UserMetrics, String> {
    List<UserMetrics> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<UserMetrics> findFirstByUserIdOrderByCreatedAtDesc(String userId);
}
