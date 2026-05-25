package mealyummy.mealservice.controller.dashboard.subscription_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.service.subscription.BundleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bundles")
@RequiredArgsConstructor
public class BundleController {

    private final BundleService bundleService;

    @PreAuthorize("hasAuthority('VIEW_ALL_BUNDLE')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<Bundle>>> getAllBundles(Pageable pageable) {
        Page<Bundle> bundles = bundleService.getAllBundles(pageable);
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy danh sách các gói dịch vụ thành công", bundles));
    }

    @PreAuthorize("hasAuthority('VIEW_BUNDLE')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Bundle>> getBundle(@PathVariable String id) {
        Bundle bundle = bundleService.getBundleById(id);
        return ResponseEntity.ok(BaseApiResponse.ok("Thông tin gói dịch vụ", bundle));
    }

    @PreAuthorize("hasAuthority('CREATE_BUNDLE')")
    @PostMapping
    public ResponseEntity<BaseApiResponse<Bundle>> createBundle(@Valid @RequestBody Bundle bundle) {
        Bundle created = bundleService.createBundle(bundle);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok("Tạo gói dịch vụ thành công", created));
    }

    @PreAuthorize("hasAuthority('UPDATE_BUNDLE')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Bundle>> updateBundle(@PathVariable String id, @Valid @RequestBody Bundle bundle) {
        Bundle updated = bundleService.updateBundle(id, bundle);
        return ResponseEntity.ok(BaseApiResponse.ok("Cập nhật gói dịch vụ thành công", updated));
    }

    @PreAuthorize("hasAuthority('UPDATE_BUNDLE')")
    @PutMapping("/{id}/state")
    public ResponseEntity<BaseApiResponse<Void>> changeState(@PathVariable String id) {
        String msg = bundleService.changeState(id);
        return ResponseEntity.ok(BaseApiResponse.ok(msg, null));
    }
}
