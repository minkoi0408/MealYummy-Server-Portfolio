package mealyummy.mealservice.model.entity.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Document(collection = "daily_meal_records")
public class DailyMealRecord {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("record_date")
    private String recordDate; // Format: YYYY-MM-DD

    @Field("breakfast_done")
    @Builder.Default
    private boolean breakfastDone = false;

    @Field("lunch_done")
    @Builder.Default
    private boolean lunchDone = false;

    @Field("dinner_done")
    @Builder.Default
    private boolean dinnerDone = false;

    @Field("snack_done")
    @Builder.Default
    private boolean snackDone = false;

    @Field("is_completed")
    @Builder.Default
    private boolean isCompleted = false;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
