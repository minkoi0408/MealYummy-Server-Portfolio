package mealyummy.mealservice.service.meal;

import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;

import java.util.List;

public interface MealService {
    MealResponseDTO create(MealRequestDTO request);
    List<MealResponseDTO> initHealthyMealsData();
}
