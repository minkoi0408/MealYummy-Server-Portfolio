package mealyummy.mealservice.model.repository.subscription;

import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentHistoryRepository extends MongoRepository<PaymentHistory, String> {
    Optional<PaymentHistory> findByTransactionId(String transactionId);
}
