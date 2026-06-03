package mealyummy.mealservice.service.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.pojo.RoadmapPhase;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DietRoadmapDTO {
    private String id;
    private String durationLabel;
    private double bmi;
    private String bmiCategory;
    private String bmiLabel;
    private List<String> diseases;
    private String overallGoal;
    private String summary;
    private List<RoadmapPhase> phases;
    private String status;
    private Instant generatedAt;
    private Instant updatedAt;
}
