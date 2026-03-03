package mealyummy.mealservice.service.meal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NutritionDTO {

    @Min(value = 0, message = "Calories không được là số âm")
    @Schema(description = "Năng lượng (kcal)", example = "450")
    private Double calories;

    @Min(value = 0, message = "Protein không được là số âm")
    @Schema(description = "Chất đạm (g)", example = "25.5")
    private Double protein;

    @Min(value = 0, message = "Carbs không được là số âm")
    @Schema(description = "Tinh bột (g)", example = "40")
    private Double carbs;

    @Min(value = 0, message = "Fat không được là số âm")
    @Schema(description = "Chất béo (g)", example = "15")
    private Double fat;

    @Min(value = 0, message = "Fiber không được là số âm")
    @Schema(description = "Chất xơ (g)", example = "5")
    private Double fiber;
}