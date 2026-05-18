package mealyummy.mealservice.service.subscription.dto;

import lombok.Data;

@Data
public class MockPurchaseRequest {
    private String userId;
    private String bundleId;
    private String durationCode;
}
