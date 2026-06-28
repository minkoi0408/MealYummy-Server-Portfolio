package mealyummy.mealservice.model.repository.subscription;

import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.model.enums.SubscriptionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends MongoRepository<UserSubscription, String> {
    Optional<UserSubscription> findByUserIdAndStatus(String userId, SubscriptionStatus status);
    List<UserSubscription> findByStatusAndEndDateBefore(SubscriptionStatus status, Instant date);
}
