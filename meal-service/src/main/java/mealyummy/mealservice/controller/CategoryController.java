package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.category.CategoryService;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<BaseApiResponse<CategoryDTO>> addCategory(@RequestBody CategoryDTO request) {
        CategoryDTO response = categoryService.create(request);
        String msg = "Tạo mới danh mục " + request.getName() +" món thành công";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(msg, response)
                );
    }

    @GetMapping
    public ResponseEntity<BaseApiResponse<List<CategoryDTO>>> getCategories() {
        List<CategoryDTO> response = categoryService.getAll();
        String msg = "Danh sách danh mục";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response)
             );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<CategoryDTO>> getCategories(@PathVariable String id) {
        CategoryDTO response = categoryService.get(id);
        String msg = "Thông tin danh mục";
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, response)
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<CategoryDTO>> changeState(@PathVariable String id) {
        String msg = categoryService.changeState(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null)
                );
    }
}