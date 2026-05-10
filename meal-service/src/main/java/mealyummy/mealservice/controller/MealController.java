package mealyummy.mealservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.meal.MealService;
import mealyummy.mealservice.service.meal.dto.MealRequestDTO;
import mealyummy.mealservice.service.meal.dto.MealResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    @PostMapping
    public ResponseEntity<BaseApiResponse<MealResponseDTO>> createMeal(
            @Valid @RequestBody MealRequestDTO request) {

        MealResponseDTO response = mealService.create(request);

        String msg = "Tạo mới món ăn " + request.getName() + " thành công";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @GetMapping("/import/healthy-meals")
    public ResponseEntity<BaseApiResponse<java.util.List<MealResponseDTO>>> initHealthyMealsDatabase() {
        java.util.List<MealResponseDTO> response = mealService.initHealthyMealsData();
        String msg = "Khởi tạo thành công " + response.size() + " món ăn Healthy & Gym Focus!";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }
}