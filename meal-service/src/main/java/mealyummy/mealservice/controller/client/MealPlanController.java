package mealyummy.mealservice.controller.client;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.mealplan.MealPlanService;
import mealyummy.mealservice.service.mealplan.dto.AddMealPlanRequest;
import mealyummy.mealservice.service.mealplan.dto.MealPlanDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meal-plan")
@RequiredArgsConstructor
public class MealPlanController {

    private final MealPlanService mealPlanService;

    @GetMapping
    public ResponseEntity<BaseApiResponse<List<MealPlanDTO>>> getMealPlan(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy thực đơn thành công", mealPlanService.getMealPlan(username)));
    }

    @PostMapping
    public ResponseEntity<BaseApiResponse<MealPlanDTO>> addMealPlanItem(
            Authentication authentication,
            @RequestBody AddMealPlanRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(BaseApiResponse.ok("Thêm vào thực đơn thành công", mealPlanService.addMealPlanItem(username, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> removeMealPlanItem(
            Authentication authentication,
            @PathVariable String id) {
        String username = authentication.getName();
        mealPlanService.removeMealPlanItem(username, id);
        return ResponseEntity.ok(BaseApiResponse.ok("Xóa món khỏi thực đơn thành công", null));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<BaseApiResponse<MealPlanDTO>> toggleMealStatus(
            Authentication authentication,
            @PathVariable String id) {
        String username = authentication.getName();
        return ResponseEntity.ok(BaseApiResponse.ok("Cập nhật trạng thái thành công", mealPlanService.toggleMealStatus(username, id)));
    }

    @DeleteMapping
    public ResponseEntity<BaseApiResponse<Void>> clearMealPlan(Authentication authentication) {
        String username = authentication.getName();
        mealPlanService.clearMealPlan(username);
        return ResponseEntity.ok(BaseApiResponse.ok("Xóa toàn bộ thực đơn thành công", null));
    }

    @DeleteMapping("/date/{date}")
    public ResponseEntity<BaseApiResponse<Void>> clearMealPlanForDate(Authentication authentication, @PathVariable String date) {
        String username = authentication.getName();
        mealPlanService.clearMealPlanForDate(username, date);
        return ResponseEntity.ok(BaseApiResponse.ok("Xóa thực đơn ngày " + date + " thành công", null));
    }

    @PostMapping("/date/{date}/sync")
    public ResponseEntity<BaseApiResponse<Void>> syncMealPlanForDate(Authentication authentication, @PathVariable String date) {
        String username = authentication.getName();
        mealPlanService.syncMealPlanForDate(username, date);
        return ResponseEntity.ok(BaseApiResponse.ok("Đã tạo thực đơn cho ngày " + date, null));
    }
}
