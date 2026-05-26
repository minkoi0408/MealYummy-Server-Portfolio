package mealyummy.mealservice.controller.dashboard.subscription_management;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.service.subscription.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-subscriptions")
@RequiredArgsConstructor
public class UserSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PreAuthorize("hasAuthority('VIEW_ALL_USER_SUBSCRIPTION')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<UserSubscription>>> getAllSubscriptions(Pageable pageable) {
        Page<UserSubscription> response = subscriptionService.getAllSubscriptions(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách người dùng đăng ký gói thành công", response));
    }

    @PreAuthorize("hasAuthority('VIEW_USER_SUBSCRIPTION')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<UserSubscription>> getSubscription(@PathVariable String id) {
        UserSubscription response = subscriptionService.getSubscriptionById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thông tin đăng ký gói", response));
    }
}
