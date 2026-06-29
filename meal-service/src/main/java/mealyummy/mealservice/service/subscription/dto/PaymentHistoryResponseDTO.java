package mealyummy.mealservice.service.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.enums.PaymentStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponseDTO {
    private String id;
    private String txId;
    private String userId;
    private String bundleId;
    private String durationCode;
    private double amount;
    private PaymentStatus paymentStatus;
    private String transactionId;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String username;
    private String bundleName;
}
