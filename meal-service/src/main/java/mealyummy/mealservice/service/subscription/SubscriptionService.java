package mealyummy.mealservice.service.subscription;

import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.service.subscription.dto.MockPurchaseRequest;

public interface SubscriptionService {
    UserSubscription mockPurchase(MockPurchaseRequest request);
    void processSuccessfulPayment(String userId, String bundleId, String durationCode);
    void processExpiredSubscriptions();
}
