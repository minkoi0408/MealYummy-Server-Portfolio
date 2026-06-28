package mealyummy.mealservice.service.subscription.dto;

import lombok.Data;
import mealyummy.mealservice.model.enums.PaymentMethod;

@Data
public class PaymentCreateRequest {
    private String userId;
    private String bundleId;
    private String durationCode;
    private PaymentMethod paymentMethod;
}
