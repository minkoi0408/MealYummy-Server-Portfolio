package mealyummy.mealservice.service.mealplan;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.MealPlanItem;
import mealyummy.mealservice.model.entity.auth.User;
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
    private final mealyummy.mealservice.service.roadmap.DietRoadmapService dietRoadmapService;
    private final mealyummy.mealservice.model.repository.DietRoadmapRepository dietRoadmapRepository;

    public List<MealPlanDTO> getMealPlan(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<MealPlanItem> items = mealPlanRepository.findByUserIdOrderByDateDesc(user.getId());
        
        // Auto-generate 3 days if completely empty for the next 3 days
        java.time.LocalDate today = java.time.LocalDate.now();
        List<String> next3Days = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            next3Days.add(today.plusDays(i).toString());
        }
        
        boolean hasMealsForNext3Days = items.stream()
                .anyMatch(item -> next3Days.contains(item.getDate()));

        if (!hasMealsForNext3Days) {
            List<mealyummy.mealservice.model.entity.DietRoadmap> roadmaps = dietRoadmapRepository.findAllByUserIdOrderByGeneratedAtDesc(user.getId());
            if (!roadmaps.isEmpty()) {
                mealyummy.mealservice.model.entity.DietRoadmap activeRoadmap = roadmaps.get(0); // get latest
                try {
                    dietRoadmapService.syncToMealPlan(username, activeRoadmap.getId(), 3);
                    items = mealPlanRepository.findByUserIdOrderByDateDesc(user.getId());
                } catch (Exception e) {
                    // Ignore error if generation fails, just return empty list
                }
            }
        }
        
        return items.stream()
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
                .isEaten(false)
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

    public MealPlanDTO toggleMealStatus(String username, String id) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        MealPlanItem item = mealPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        
        if (!item.getUserId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to modify this item");
        }
        
        item.setEaten(!item.isEaten());
        MealPlanItem saved = mealPlanRepository.save(item);
        return mapToDTO(saved);
    }

    public void clearMealPlan(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        mealPlanRepository.deleteByUserId(user.getId());
    }

    public void clearMealPlanForDate(String username, String date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        mealPlanRepository.deleteByUserIdAndDate(user.getId(), date);
    }

    public void syncMealPlanForDate(String username, String date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<mealyummy.mealservice.model.entity.DietRoadmap> roadmaps = dietRoadmapRepository.findAllByUserIdOrderByGeneratedAtDesc(user.getId());
        if (roadmaps.isEmpty()) {
            throw new RuntimeException("Bạn cần tạo Lộ trình (Roadmap) trước khi đồng bộ thực đơn");
        }
        mealyummy.mealservice.model.entity.DietRoadmap activeRoadmap = roadmaps.get(0);
        dietRoadmapService.syncToMealPlanForDate(username, activeRoadmap.getId(), date);
    }

    private MealPlanDTO mapToDTO(MealPlanItem item) {
        return MealPlanDTO.builder()
                .id(item.getId())
                .date(item.getDate())
                .mealType(item.getMealType())
                .isEaten(item.isEaten())
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
