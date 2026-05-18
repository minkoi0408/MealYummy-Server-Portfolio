package mealyummy.mealservice.model.entity.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.enums.PaymentStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payment_histories")
public class PaymentHistory {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("bundle_id")
    private String bundleId;

    @Field("duration_code")
    private String durationCode;

    @Field("amount")
    private double amount;

    @Field("payment_status")
    private PaymentStatus paymentStatus;

    @Field("transaction_id")
    private String transactionId;

    @Field("paid_at")
    private Instant paidAt;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
