package mealyummy.mealservice.service.favorite;

import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface FavoriteMealService {
    boolean toggleFavorite(String userId, String mealId);
    boolean isFavorite(String userId, String mealId);
    List<String> getFavoriteMealIds(String userId);
    Page<MealResponseDTO> getFavoriteMeals(String userId, Pageable pageable);
}
