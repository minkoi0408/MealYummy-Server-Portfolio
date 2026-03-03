package mealyummy.mealservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;
import mealyummy.mealservice.service.ingredient.IngredientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @PostMapping
    public ResponseEntity<BaseApiResponse<IngredientDTO>> addIngredient(@RequestBody IngredientDTO request) {
        IngredientDTO response = ingredientService.create(request);

        String msg = "Tạo mới nguyên liệu " + request.getName() + " thành công";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @GetMapping
    public ResponseEntity<BaseApiResponse<List<IngredientDTO>>> getAllIngredients() {
        List<IngredientDTO> response = ingredientService.getAll();

        String msg = "Lấy danh sách nguyên liệu thành công";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BaseApiResponse<List<IngredientDTO>>> addIngredientsBulk(@RequestBody List<IngredientDTO> requests) {

        List<IngredientDTO> response = ingredientService.createBulk(requests);

        String msg = "Đã tạo mới thành công " + response.size() + " nguyên liệu";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }
}