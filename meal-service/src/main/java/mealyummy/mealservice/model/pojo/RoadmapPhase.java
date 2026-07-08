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

    // ─── Phase timing & status (added for phase-lock feature) ─────────────────
    private String startDate;   // yyyy-MM-dd, ngày bắt đầu thực tế
    private String endDate;     // yyyy-MM-dd, ngày kết thúc thực tế
    /** "LOCKED" | "ACTIVE" | "PENDING_REVIEW" | "COMPLETED" */
    private String phaseStatus;
    /** Cân nặng mục tiêu AI đặt ra cho phase này (kg) */
    private double targetWeight;
}
