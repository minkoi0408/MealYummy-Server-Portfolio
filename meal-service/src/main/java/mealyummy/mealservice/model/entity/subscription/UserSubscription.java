package mealyummy.mealservice.model.entity.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.enums.SubscriptionStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_subscriptions")
public class UserSubscription {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("role")
    private String role;

    @Field("bundle_id")
    private String bundleId;

    @Field("duration_code")
    private String durationCode;

    @Field("start_date")
    private Instant startDate;

    @Field("end_date")
    private Instant endDate;

    @Field("status")
    private SubscriptionStatus status;

    @Field("auto_renew")
    @Builder.Default
    private boolean autoRenew = false;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
    
    @Transient
    private String username;
    
    @Transient
    private String bundleName;
}
