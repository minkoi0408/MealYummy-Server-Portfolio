package mealyummy.mealservice.model.entity.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_metrics")
public class UserMetrics {
    @Id
    private String id;

    @Indexed
    private String userId;

    private double weight;
    private double height;
    private int age;
    private String gender;
    private String activity;
    private String goal;
    private Double bodyFat;
    private Double muscleMass;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
