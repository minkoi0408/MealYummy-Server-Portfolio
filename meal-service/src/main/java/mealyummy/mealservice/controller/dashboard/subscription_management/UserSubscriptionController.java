package mealyummy.mealservice.controller.dashboard.subscription_management;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.service.subscription.SubscriptionService;
import mealyummy.mealservice.service.subscription.dto.UserSubscriptionResponseDTO;
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

    @PreAuthorize("@apiAuth.check()")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<UserSubscriptionResponseDTO>>> getAllSubscriptions(Pageable pageable) {
        Page<UserSubscriptionResponseDTO> response = subscriptionService.getAllSubscriptions(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lay danh sach dang ky thanh cong", response));
    }

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<UserSubscription>> getSubscription(@PathVariable String id) {
        UserSubscription response = subscriptionService.getSubscriptionById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thong tin dang ky", response));
    }
}
