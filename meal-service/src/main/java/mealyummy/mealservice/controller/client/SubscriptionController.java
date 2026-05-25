package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.model.enums.SubscriptionStatus;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.model.repository.subscription.UserSubscriptionRepository;
import mealyummy.mealservice.service.subscription.SubscriptionService;
import mealyummy.mealservice.service.subscription.dto.MockPurchaseRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;

    @PostMapping("/mock-purchase")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseApiResponse<UserSubscription>> mockPurchase(@RequestBody MockPurchaseRequest request) {
        UserSubscription subscription = subscriptionService.mockPurchase(request);
        return ResponseEntity.ok(BaseApiResponse.ok("Thanh toán thành công. Đã nâng cấp Membership.", subscription));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseApiResponse<UserSubscription>> getCurrentSubscription(Authentication authentication) {
        String username = authentication.getName();
        
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserSubscription subscription = userSubscriptionRepository
                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
                .orElse(null);

        return ResponseEntity.ok(BaseApiResponse.ok("Lấy thông tin gói cước thành công", subscription));
    }
}
