package mealyummy.mealservice.service.favorite;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.model.entity.food.FavoriteMeal;
import mealyummy.mealservice.model.repository.FavoriteMealRepository;
import mealyummy.mealservice.service.meal.MealService;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteMealServiceImpl implements FavoriteMealService {

    private final FavoriteMealRepository favoriteMealRepository;
    private final MealService mealService;

    @Override
    public boolean toggleFavorite(String userId, String mealId) {
        Optional<FavoriteMeal> existing = favoriteMealRepository.findByUserIdAndMealId(userId, mealId);
        if (existing.isPresent()) {
            favoriteMealRepository.delete(existing.get());
            return false; // Removed from favorites
        } else {
            FavoriteMeal fav = FavoriteMeal.builder()
                    .userId(userId)
                    .mealId(mealId)
                    .createdAt(Instant.now())
                    .build();
            favoriteMealRepository.save(fav);
            return true; // Added to favorites
        }
    }

    @Override
    public boolean isFavorite(String userId, String mealId) {
        return favoriteMealRepository.findByUserIdAndMealId(userId, mealId).isPresent();
    }

    @Override
    public List<String> getFavoriteMealIds(String userId) {
        return favoriteMealRepository.findMealIdsByUserId(userId).stream()
                .map(FavoriteMeal::getMealId)
                .collect(Collectors.toList());
    }

    @Override
    public Page<MealResponseDTO> getFavoriteMeals(String userId, Pageable pageable) {
        Page<FavoriteMeal> favoritePage = favoriteMealRepository.findByUserId(userId, pageable);
        
        List<MealResponseDTO> meals = new ArrayList<>();
        for (FavoriteMeal fav : favoritePage.getContent()) {
            try {
                meals.add(mealService.get(fav.getMealId()));
            } catch (AppException e) {
                log.warn("Meal {} not found but exists in favorites for user {}", fav.getMealId(), userId);
                // Optionally remove the orphaned favorite
                // favoriteMealRepository.delete(fav);
            }
        }
        
        return new PageImpl<>(meals, pageable, favoritePage.getTotalElements());
    }
}
