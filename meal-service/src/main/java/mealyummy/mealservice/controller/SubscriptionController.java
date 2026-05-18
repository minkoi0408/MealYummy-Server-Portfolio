package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.service.subscription.SubscriptionService;
import mealyummy.mealservice.service.subscription.dto.MockPurchaseRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/mock-purchase")
    @PreAuthorize("isAuthenticated()") // Hoặc role cần thiết nếu có
    public ResponseEntity<BaseApiResponse<UserSubscription>> mockPurchase(@RequestBody MockPurchaseRequest request) {
        UserSubscription subscription = subscriptionService.mockPurchase(request);
        return ResponseEntity.ok(BaseApiResponse.ok("Thanh toán thành công. Đã nâng cấp Membership.", subscription));
    }
}
