package mealyummy.mealservice.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nutrition {

    private Double calories;

    private Double protein;

    private Double carbs;

    private Double fat;

    private Double fiber;
}