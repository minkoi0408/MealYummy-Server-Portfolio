package mealyummy.mealservice.service.roadmap.dto;

import lombok.Data;

@Data
public class GenerateRoadmapRequest {
    private String durationLabel; // "1_MONTH" | "3_MONTHS" | "6_MONTHS" | "12_MONTHS"
}
