package mealyummy.mealservice.controller.dashboard.food;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.ingredient.IngredientService;
import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @PreAuthorize("hasAuthority('CREATE_INGREDIENT')")
    @PostMapping
    public ResponseEntity<BaseApiResponse<IngredientDTO>> addIngredient(@Valid @RequestBody IngredientDTO request) {
        IngredientDTO response = ingredientService.create(request);
        String msg = "Tạo mới nguyên liệu " + request.getName() + " thành công";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_INGREDIENT')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<IngredientDTO>>> getAllIngredients(Pageable pageable) {
        Page<IngredientDTO> response = ingredientService.getAll(pageable);
        String msg = "Lấy danh sách nguyên liệu thành công";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_INGREDIENT')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<IngredientDTO>> getIngredient(@PathVariable String id) {
        IngredientDTO response = ingredientService.get(id);
        String msg = "Thông tin nguyên liệu";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('UPDATE_INGREDIENT')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<IngredientDTO>> updateIngredient(@PathVariable String id, @Valid @RequestBody IngredientDTO request) {
        IngredientDTO response = ingredientService.update(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật nguyên liệu thành công", response));
    }

    @PreAuthorize("hasAuthority('UPDATE_INGREDIENT')")
    @PutMapping("/{id}/state")
    public ResponseEntity<BaseApiResponse<Void>> changeState(@PathVariable String id) {
        String msg = ingredientService.changeState(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @PreAuthorize("hasAuthority('DELETE_INGREDIENT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> deleteIngredient(@PathVariable String id) {
        ingredientService.delete(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa nguyên liệu thành công", null));
    }

    @PreAuthorize("hasAuthority('DELETE_INGREDIENT')")
    @DeleteMapping("/bulk")
    public ResponseEntity<BaseApiResponse<Void>> deleteIngredientsBulk(@RequestBody List<String> ids) {
        ingredientService.deleteBulk(ids);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa danh sách nguyên liệu thành công", null));
    }

    @PreAuthorize("hasAuthority('CREATE_INGREDIENT')")
    @PostMapping("/bulk")
    public ResponseEntity<BaseApiResponse<List<IngredientDTO>>> addIngredientsBulk(@Valid @RequestBody List<IngredientDTO> requests) {
        List<IngredientDTO> response = ingredientService.createBulk(requests);
        String msg = "Đã tạo mới thành công " + response.size() + " nguyên liệu";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }
}