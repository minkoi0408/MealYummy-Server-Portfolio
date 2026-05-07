package mealyummy.mealservice.service.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMetricsDTO {
    private double weight;
    private double height;
    private int age;
    private String gender;
    private String activity;
    private String goal;
    private Double bodyFat;
    private Double muscleMass;
    private Instant updatedAt;
}
