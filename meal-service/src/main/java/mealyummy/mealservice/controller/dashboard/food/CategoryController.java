package mealyummy.mealservice.controller.dashboard.food;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.category.CategoryService;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    @PostMapping
    public ResponseEntity<BaseApiResponse<CategoryDTO>> addCategory(@Valid @RequestBody CategoryDTO request) {
        CategoryDTO response = categoryService.create(request);
        String msg = "Tạo mới danh mục " + request.getName() + " thành công";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_ALL_CATEGORY')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<CategoryDTO>>> getCategories(Pageable pageable) {
        Page<CategoryDTO> response = categoryService.getAll(pageable);
        String msg = "Danh sách danh mục";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('VIEW_CATEGORY')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<CategoryDTO>> getCategory(@PathVariable String id) {
        CategoryDTO response = categoryService.get(id);
        String msg = "Thông tin danh mục";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('UPDATE_CATEGORY')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<CategoryDTO>> updateCategory(@PathVariable String id, @Valid @RequestBody CategoryDTO request) {
        CategoryDTO response = categoryService.update(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật danh mục thành công", response));
    }

    @PreAuthorize("hasAuthority('UPDATE_CATEGORY')")
    @PutMapping("/{id}/state")
    public ResponseEntity<BaseApiResponse<Void>> changeState(@PathVariable String id) {
        String msg = categoryService.changeState(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @PreAuthorize("hasAuthority('DELETE_CATEGORY')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> deleteCategory(@PathVariable String id) {
        categoryService.delete(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa danh mục thành công", null));
    }

    @PreAuthorize("hasAuthority('DELETE_CATEGORY')")
    @DeleteMapping("/bulk")
    public ResponseEntity<BaseApiResponse<Void>> deleteCategoriesBulk(@RequestBody List<String> ids) {
        categoryService.deleteBulk(ids);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa danh sách danh mục thành công", null));
    }

    @PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    @PostMapping("/bulk")
    public ResponseEntity<BaseApiResponse<List<CategoryDTO>>> addCategoriesBulk(@Valid @RequestBody List<CategoryDTO> requests) {
        List<CategoryDTO> response = categoryService.createBulk(requests);
        String msg = "Khởi tạo thành công danh sách danh mục";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }

    @PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    @PostMapping("/nested-bulk")
    public ResponseEntity<BaseApiResponse<List<CategoryDTO>>> addNestedCategoriesBulk(@Valid @RequestBody List<CategoryDTO> requests) {
        List<CategoryDTO> response = categoryService.createNestedBulk(requests);
        String msg = "Khởi tạo thành công cấu trúc danh mục nhiều tầng";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response));
    }
}