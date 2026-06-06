package mealyummy.mealservice.controller.dashboard.food;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.meal.MealService;
import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PreAuthorize("hasAuthority('CREATE_MEAL')")
    @PostMapping
    public ResponseEntity<BaseApiResponse<MealResponseDTO>> createMeal(
            @Valid @RequestBody MealRequestDTO request) {
        MealResponseDTO response = mealService.create(request);
        String msg = "Tạo mới món ăn " + request.getName() + " thành công";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_MEAL')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<MealResponseDTO>>> getAllMeals(Pageable pageable) {
        Page<MealResponseDTO> response = mealService.getAll(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách món ăn thành công", response));
    }

    @PreAuthorize("hasAuthority('VIEW_MEAL')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<MealResponseDTO>> getMeal(@PathVariable String id) {
        MealResponseDTO response = mealService.get(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thông tin món ăn", response));
    }

    @PreAuthorize("hasAuthority('UPDATE_MEAL')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<MealResponseDTO>> updateMeal(@PathVariable String id, @Valid @RequestBody MealRequestDTO request) {
        MealResponseDTO response = mealService.update(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật món ăn thành công", response));
    }

    @PreAuthorize("hasAuthority('DELETE_MEAL')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> deleteMeal(@PathVariable String id) {
        mealService.delete(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa món ăn thành công", null));
    }

    @PreAuthorize("hasAuthority('DELETE_MEAL')")
    @DeleteMapping("/bulk")
    public ResponseEntity<BaseApiResponse<Void>> deleteMealsBulk(@RequestBody List<String> ids) {
        mealService.deleteBulk(ids);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa danh sách món ăn thành công", null));
    }

    @PreAuthorize("hasAuthority('CREATE_MEAL')")
    @PostMapping("/bulk")
    public ResponseEntity<BaseApiResponse<List<MealResponseDTO>>> createMealsBulk(@Valid @RequestBody List<MealRequestDTO> requests) {
        List<MealResponseDTO> response = mealService.createBulk(requests);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok("Tạo nhiều món ăn thành công", response));
    }

    @PreAuthorize("hasAuthority('CREATE_MEAL')")
    @GetMapping("/import/healthy-meals")
    public ResponseEntity<BaseApiResponse<java.util.List<MealResponseDTO>>> initHealthyMealsDatabase() {
        java.util.List<MealResponseDTO> response = mealService.initHealthyMealsData();
        String msg = "Khởi tạo thành công " + response.size() + " món ăn Healthy & Gym Focus!";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('UPDATE_MEAL')")
    @PostMapping("/{id}/images")
    public ResponseEntity<BaseApiResponse<MealResponseDTO>> uploadMealImage(
            @PathVariable String id, 
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        MealResponseDTO response = mealService.uploadMealImage(id, file);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Upload ảnh món ăn thành công", response));
    }
}