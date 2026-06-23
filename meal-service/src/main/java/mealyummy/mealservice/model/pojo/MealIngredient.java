package mealyummy.mealservice.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.enums.IngredientUnit;
import mealyummy.mealservice.service.meal.dto.MealIngredientDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MealIngredient {

    private String name;

    private Double value;
    private IngredientUnit unit;

    public MealIngredientDTO convert(){
        return MealIngredientDTO.builder()
                .name(this.name)
                .value(this.value)
                .unitDesc(this.unit.getDescription())
                .build();
    }
}
