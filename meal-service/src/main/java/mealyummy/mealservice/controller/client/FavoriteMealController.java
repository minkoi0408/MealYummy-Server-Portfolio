package mealyummy.mealservice.controller.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.favorite.FavoriteMealService;
import mealyummy.mealservice.service.favorite.dto.FavoriteStatusResponseDTO;
import mealyummy.mealservice.service.favorite.dto.ToggleFavoriteResponseDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "Client - Favorite Meals", description = "APIs for managing user's favorite meals")
public class FavoriteMealController {

    private final FavoriteMealService favoriteMealService;

    @PostMapping("/{mealId}/toggle")
    @Operation(summary = "Toggle favorite status for a meal")
    public ResponseEntity<BaseApiResponse<ToggleFavoriteResponseDTO>> toggleFavorite(
            @PathVariable String mealId,
            Authentication authentication) {
        
        String username = authentication.getName();
        boolean isFavorite = favoriteMealService.toggleFavorite(username, mealId);
        String message = isFavorite ? "Đã thêm vào danh sách yêu thích" : "Đã bỏ khỏi danh sách yêu thích";
        
        return ResponseEntity.ok(BaseApiResponse.ok(message, new ToggleFavoriteResponseDTO(isFavorite, message)));
    }

    @GetMapping("/check/{mealId}")
    @Operation(summary = "Check if a meal is favorited by the current user")
    public ResponseEntity<BaseApiResponse<FavoriteStatusResponseDTO>> checkFavoriteStatus(
            @PathVariable String mealId,
            Authentication authentication) {
            
        String username = authentication.getName();
        boolean isFavorite = favoriteMealService.isFavorite(username, mealId);
        
        return ResponseEntity.ok(BaseApiResponse.ok("Kiểm tra trạng thái yêu thích thành công", new FavoriteStatusResponseDTO(isFavorite)));
    }

    @GetMapping("/ids")
    @Operation(summary = "Get list of all favorite meal IDs for the current user")
    public ResponseEntity<BaseApiResponse<List<String>>> getFavoriteMealIds(
            Authentication authentication) {
            
        String username = authentication.getName();
        List<String> mealIds = favoriteMealService.getFavoriteMealIds(username);
        
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy danh sách ID món ăn yêu thích thành công", mealIds));
    }

    @GetMapping
    @Operation(summary = "Get paginated list of favorite meals")
    public ResponseEntity<BaseApiResponse<Page<MealResponseDTO>>> getFavoriteMeals(
            Pageable pageable,
            Authentication authentication) {
            
        String username = authentication.getName();
        Page<MealResponseDTO> meals = favoriteMealService.getFavoriteMeals(username, pageable);
        
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy danh sách món ăn yêu thích thành công", meals));
    }
}
