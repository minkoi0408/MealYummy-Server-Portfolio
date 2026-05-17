package mealyummy.mealservice.service.mealplan.dto;

import lombok.Data;

@Data
public class AddMealPlanRequest {
    private MealPlanDTO.RecipeDTO recipe;
    private String date;
    private String mealType;
}
