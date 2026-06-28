package mealyummy.mealservice.service.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.model.enums.PaymentStatus;
import mealyummy.mealservice.model.enums.SubscriptionStatus;
import mealyummy.mealservice.model.pojo.BundleDuration;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.model.repository.subscription.PaymentHistoryRepository;
import mealyummy.mealservice.model.repository.subscription.UserSubscriptionRepository;
import mealyummy.mealservice.service.subscription.dto.MockPurchaseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final BundleService bundleService;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private static final String ROLE_MEMBERSHIP = "ROLE_MEMBERSHIP";
    private static final String ROLE_FREE = "ROLE_FREE";

    @Override
    @Transactional
    public UserSubscription mockPurchase(MockPurchaseRequest request) {
        // Mock purchase now just delegates to processSuccessfulPayment
        // It bypasses the payment gateway for testing
        
        Bundle bundle = bundleService.getBundleById(request.getBundleId());
        BundleDuration chosenDuration = bundle.getDurations().stream()
                .filter(d -> d.getDurationCode().equals(request.getDurationCode()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.DURATION_INVALID));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PaymentHistory payment = PaymentHistory.builder()
                .userId(user.getId())
                .bundleId(bundle.getId())
                .durationCode(chosenDuration.getDurationCode())
                .amount(chosenDuration.getPrice())
                .paymentStatus(PaymentStatus.SUCCESS)
                .transactionId(UUID.randomUUID().toString())
                .paidAt(Instant.now())
                .build();
        paymentHistoryRepository.save(payment);

        processSuccessfulPayment(user.getId(), bundle.getId(), chosenDuration.getDurationCode());
        
        return userSubscriptionRepository.findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE).get();
    }

    @Override
    @Transactional
    public void processSuccessfulPayment(String userId, String bundleId, String durationCode) {
        Bundle bundle = bundleService.getBundleById(bundleId);
        BundleDuration chosenDuration = bundle.getDurations().stream()
                .filter(d -> d.getDurationCode().equals(durationCode))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.DURATION_INVALID));
                
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 3. Update or Create UserSubscription
        UserSubscription subscription = userSubscriptionRepository
                .findByUserIdAndStatus(user.getId(), SubscriptionStatus.ACTIVE)
                .orElse(UserSubscription.builder()
                        .userId(user.getId())
                        .role(ROLE_MEMBERSHIP)
                        .status(SubscriptionStatus.ACTIVE)
                        .startDate(Instant.now())
                        .endDate(Instant.now())
                        .build());

        // Accumulate endDate if already active
        Instant newEndDate = (subscription.getEndDate().isAfter(Instant.now()) 
                ? subscription.getEndDate() 
                : Instant.now())
                .plus(chosenDuration.getDurationInDays(), ChronoUnit.DAYS);

        subscription.setEndDate(newEndDate);
        userSubscriptionRepository.save(subscription);

        // 4. Update User Role
        updateUserRole(user, ROLE_MEMBERSHIP);
    }

    @Override
    @Transactional
    public void processExpiredSubscriptions() {
        List<UserSubscription> expiredSubscriptions = userSubscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, Instant.now());

        for (UserSubscription sub : expiredSubscriptions) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            userSubscriptionRepository.save(sub);

            userRepository.findById(sub.getUserId()).ifPresent(user -> {
                updateUserRole(user, ROLE_FREE);
                log.info("User {} subscription expired, reverted to ROLE_FREE", user.getId());
            });
        }
    }

    private void updateUserRole(User user, String roleCode) {
        Role role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public org.springframework.data.domain.Page<UserSubscription> getAllSubscriptions(org.springframework.data.domain.Pageable pageable) {
        return userSubscriptionRepository.findAll(pageable);
    }

    @Override
    public UserSubscription getSubscriptionById(String id) {
        return userSubscriptionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
    }
}
