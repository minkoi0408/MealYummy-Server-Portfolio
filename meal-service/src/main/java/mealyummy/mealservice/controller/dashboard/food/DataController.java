package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.DataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataController {
    private final DataService dataInitService;

    @GetMapping("/import/categories")
    public ResponseEntity<BaseApiResponse<String>> initCategoryDatabase() {
        String msg = dataInitService.initCategoryData();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @GetMapping("/import/ingredients")
    public ResponseEntity<BaseApiResponse<String>> initIngredientDatabase() {
        String msg = dataInitService.initIngredientData();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @GetMapping("/import/tags")
    public ResponseEntity<BaseApiResponse<String>> initTagDatabase() {
        String msg = dataInitService.initTagData();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @GetMapping("/export/ingredients")
    public ResponseEntity<BaseApiResponse<String>> exportIngredients() {
        return ResponseEntity.ok(BaseApiResponse.ok(dataInitService.exportIngredientsToJson(), null));
    }

    @GetMapping("/export/tags")
    public ResponseEntity<BaseApiResponse<String>> exportTags() {
        return ResponseEntity.ok(BaseApiResponse.ok(dataInitService.exportTagToJson(), null));
    }

    @GetMapping("/export/categories")
    public ResponseEntity<BaseApiResponse<String>> exportCategories() {
        return ResponseEntity.ok(BaseApiResponse.ok(dataInitService.exportCategoryToJson(), null));
    }
}
