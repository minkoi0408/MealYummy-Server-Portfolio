package mealyummy.mealservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.pojo.RoadmapPhase;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "diet_roadmaps")
public class DietRoadmap {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String durationLabel;   // "1_MONTH" | "3_MONTHS" | "6_MONTHS" | "12_MONTHS"
    private double bmi;
    private String bmiCategory;     // "UNDERWEIGHT" | "NORMAL" | "OVERWEIGHT" | "OBESE_1" | "OBESE_2"
    private String bmiLabel;        // "Thừa cân" (tiếng Việt)
    private List<String> diseases;
    private String overallGoal;
    private String summary;
    private List<RoadmapPhase> phases;
    private String status;          // "ACTIVE" | "COMPLETED" | "PAUSED"

    private Instant generatedAt;
    private Instant updatedAt;
}
