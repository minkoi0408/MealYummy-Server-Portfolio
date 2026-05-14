package mealyummy.mealservice.service.mealplan;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.MealPlanItem;
import mealyummy.mealservice.model.entity.User;
import mealyummy.mealservice.model.repository.MealPlanRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.mealplan.dto.AddMealPlanRequest;
import mealyummy.mealservice.service.mealplan.dto.MealPlanDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MealPlanService {
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;

    public List<MealPlanDTO> getMealPlan(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mealPlanRepository.findByUserIdOrderByDateDesc(user.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public MealPlanDTO addMealPlanItem(String username, AddMealPlanRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MealPlanItem item = MealPlanItem.builder()
                .userId(user.getId())
                .recipeId(request.getRecipe().getId())
                .recipeName(request.getRecipe().getName())
                .recipeImage(request.getRecipe().getImage())
                .calories(request.getRecipe().getCalories())
                .cookTime(request.getRecipe().getCookTime())
                .date(request.getDate())
                .mealType(request.getMealType())
                .createdAt(Instant.now())
                .build();

        MealPlanItem saved = mealPlanRepository.save(item);
        return mapToDTO(saved);
    }

    public void removeMealPlanItem(String username, String id) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        MealPlanItem item = mealPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        if (!item.getUserId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this item");
        }
        
        mealPlanRepository.deleteById(id);
    }

    public void clearMealPlan(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        mealPlanRepository.deleteByUserId(user.getId());
    }

    private MealPlanDTO mapToDTO(MealPlanItem item) {
        return MealPlanDTO.builder()
                .id(item.getId())
                .date(item.getDate())
                .mealType(item.getMealType())
                .recipe(MealPlanDTO.RecipeDTO.builder()
                        .id(item.getRecipeId())
                        .name(item.getRecipeName())
                        .image(item.getRecipeImage())
                        .calories(item.getCalories())
                        .cookTime(item.getCookTime())
                        .build())
                .build();
    }
}
