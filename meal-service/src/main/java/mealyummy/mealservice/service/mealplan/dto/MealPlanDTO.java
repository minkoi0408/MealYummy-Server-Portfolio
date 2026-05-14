package mealyummy.mealservice.service.mealplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealPlanDTO {
    private String id;
    private RecipeDTO recipe;
    private String date;
    private String mealType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipeDTO {
        private String id;
        private String name;
        private String image;
        private String calories;
        private String cookTime;
    }
}
