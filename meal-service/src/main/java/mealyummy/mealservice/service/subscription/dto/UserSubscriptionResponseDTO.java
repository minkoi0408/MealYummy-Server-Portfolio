package mealyummy.mealservice.service.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.enums.SubscriptionStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscriptionResponseDTO {
    private String id;
    private String userId;
    private String role;
    private String bundleId;
    private String durationCode;
    private Instant startDate;
    private Instant endDate;
    private SubscriptionStatus status;
    private boolean autoRenew;
    private Instant createdAt;
    private Instant updatedAt;
    private String username;
    private String bundleName;
}
