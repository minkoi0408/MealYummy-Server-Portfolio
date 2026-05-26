package mealyummy.mealservice.controller.client;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.user.UserService;
import mealyummy.mealservice.service.user.UserMetricsService;
import mealyummy.mealservice.service.user.dto.UserMetricsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMetricsService userMetricsService;

    @PostMapping("/avatar")
    public ResponseEntity<BaseApiResponse<String>> updateAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        String username = authentication.getName();
        String avatarUrl = userService.updateAvatar(username, file);

        return ResponseEntity.ok(BaseApiResponse.ok("Cập nhật avatar thành công", avatarUrl));
    }

    @GetMapping("/metrics")
    public ResponseEntity<BaseApiResponse<UserMetricsDTO>> getMetrics(Authentication authentication) {
        String username = authentication.getName();
        UserMetricsDTO metrics = userMetricsService.getUserMetrics(username);
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy chỉ số thành công", metrics));
    }

    @PostMapping("/metrics")
    public ResponseEntity<BaseApiResponse<Void>> updateMetrics(
            @RequestBody UserMetricsDTO dto,
            Authentication authentication) {
        String username = authentication.getName();
        userMetricsService.updateUserMetrics(username, dto);
        return ResponseEntity.ok(BaseApiResponse.ok("Lưu chỉ số sức khỏe thành công", null));
    }

    @GetMapping("/metrics/history")
    public ResponseEntity<BaseApiResponse<java.util.List<UserMetricsDTO>>> getMetricsHistory(Authentication authentication) {
        String username = authentication.getName();
        java.util.List<UserMetricsDTO> history = userMetricsService.getUserMetricsHistory(username);
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy lịch sử chỉ số thành công", history));
    }
}
