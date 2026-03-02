package mealyummy.mealservice.service.meal;

import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;

public interface MealService {
    MealResponseDTO create(MealRequestDTO request);
}
