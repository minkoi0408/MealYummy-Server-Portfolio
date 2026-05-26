package mealyummy.mealservice.service.meal;

import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MealService {
    MealResponseDTO create(MealRequestDTO request);
    Page<MealResponseDTO> getAll(Pageable pageable);
    MealResponseDTO get(String id);
    MealResponseDTO update(String id, MealRequestDTO request);
    void delete(String id);
    void deleteBulk(List<String> ids);
    List<MealResponseDTO> createBulk(List<MealRequestDTO> requests);
    List<MealResponseDTO> initHealthyMealsData();
}
