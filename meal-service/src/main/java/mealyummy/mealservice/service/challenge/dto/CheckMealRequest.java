package mealyummy.mealservice.service.challenge.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CheckMealRequest {
    @NotNull(message = "Meal type cannot be null (BREAKFAST, LUNCH, DINNER, SNACK)")
    private String mealType;
    
    @NotNull(message = "Status cannot be null")
    private Boolean status;
}
