package mealyummy.mealservice.controller.dashboard.food;

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

    @GetMapping("/export/meals")
    public ResponseEntity<BaseApiResponse<String>> exportMeals() {
        return ResponseEntity.ok(BaseApiResponse.ok(dataInitService.exportMealToJson(), null));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_CATEGORY')")
    @org.springframework.web.bind.annotation.PostMapping("/import/upload/categories")
    public ResponseEntity<BaseApiResponse<String>> importCategoriesUpload(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String msg = dataInitService.importCategoriesFromFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseApiResponse.ok(msg, null));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_TAG')")
    @org.springframework.web.bind.annotation.PostMapping("/import/upload/tags")
    public ResponseEntity<BaseApiResponse<String>> importTagsUpload(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String msg = dataInitService.importTagsFromFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseApiResponse.ok(msg, null));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_INGREDIENT')")
    @org.springframework.web.bind.annotation.PostMapping("/import/upload/ingredients")
    public ResponseEntity<BaseApiResponse<String>> importIngredientsUpload(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String msg = dataInitService.importIngredientsFromFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseApiResponse.ok(msg, null));
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('CREATE_MEAL')")
    @org.springframework.web.bind.annotation.PostMapping("/import/upload/meals")
    public ResponseEntity<BaseApiResponse<String>> importMealsUpload(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String msg = dataInitService.importMealsFromFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseApiResponse.ok(msg, null));
    }
}
