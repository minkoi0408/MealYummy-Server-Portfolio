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
@Document(collection = "user_streaks")
public class UserStreak {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("current_streak")
    @Builder.Default
    private int currentStreak = 0;

    @Field("highest_streak")
    @Builder.Default
    private int highestStreak = 0;

    @Field("last_active_date")
    private String lastActiveDate; // Format: YYYY-MM-DD

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
