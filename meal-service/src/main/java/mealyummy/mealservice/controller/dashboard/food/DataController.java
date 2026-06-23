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

    @GetMapping("/import/meals-from-json")
    public ResponseEntity<BaseApiResponse<String>> importMealsFromLocalJson() {
        String msg = dataInitService.importMealsFromLocalJson();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @org.springframework.security.access.prepost.PreAuthorize("@apiAuth.check()")
    @org.springframework.web.bind.annotation.PostMapping("/import/upload/meals")
    public ResponseEntity<BaseApiResponse<String>> importMealsUpload(@org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String msg = dataInitService.importMealsFromFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseApiResponse.ok(msg, null));
    }
}
