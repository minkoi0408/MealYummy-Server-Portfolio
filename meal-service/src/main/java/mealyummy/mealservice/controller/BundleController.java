package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.service.subscription.BundleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    @GetMapping
    public ResponseEntity<BaseApiResponse<List<Bundle>>> getAllActiveBundles() {
        List<Bundle> bundles = bundleService.getAllActiveBundles();
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy danh sách các gói dịch vụ thành công", bundles));
    }

    @PostMapping
    public ResponseEntity<BaseApiResponse<Bundle>> createBundle(@RequestBody Bundle bundle) {
        Bundle created = bundleService.createBundle(bundle);
        return ResponseEntity.ok(BaseApiResponse.ok("Tạo gói dịch vụ thành công", created));
    }
}
