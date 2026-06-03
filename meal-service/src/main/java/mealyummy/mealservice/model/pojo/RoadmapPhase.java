package mealyummy.mealservice.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapPhase {
    private int phaseNumber;
    private String phaseName;
    private String durationWeeks;
    private String startWeek;
    private String endWeek;
    private String targetGoal;
    private double targetCaloriesPerDay;
    private double targetProteinGram;
    private double targetCarbsGram;
    private double targetFatGram;
    private List<String> allowedFoods;
    private List<String> avoidedFoods;
    private List<String> recommendedMealNames;
    private String aiAdvice;
    private String breakfastTemplate;
    private String lunchTemplate;
    private String dinnerTemplate;
    private String snackTemplate;
}
