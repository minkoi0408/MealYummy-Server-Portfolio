package mealyummy.mealservice.controller.client;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.roadmap.DietRoadmapService;
import mealyummy.mealservice.service.roadmap.dto.DietRoadmapDTO;
import mealyummy.mealservice.service.roadmap.dto.GenerateRoadmapRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diet-roadmap")
@RequiredArgsConstructor
public class DietRoadmapController {

    private final DietRoadmapService dietRoadmapService;

    /**
     * POST /api/v1/diet-roadmap/generate
     * Gọi Gemini AI sinh lộ trình mới (8-15 giây).
     */
    @PostMapping("/generate")
    public ResponseEntity<BaseApiResponse<DietRoadmapDTO>> generate(
            @RequestBody GenerateRoadmapRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        DietRoadmapDTO result = dietRoadmapService.generate(username, request);
        return ResponseEntity.ok(BaseApiResponse.ok("Tạo lộ trình thành công", result));
    }

    /**
     * GET /api/v1/diet-roadmap
     * Lấy lộ trình mới nhất. 404 nếu chưa có.
     */
    @GetMapping
    public ResponseEntity<BaseApiResponse<DietRoadmapDTO>> getCurrent(Authentication authentication) {
        String username = authentication.getName();
        DietRoadmapDTO result = dietRoadmapService.getCurrent(username);
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy lộ trình thành công", result));
    }

    /**
     * GET /api/v1/diet-roadmap/history
     * Lịch sử tất cả lộ trình.
     */
    @GetMapping("/history")
    public ResponseEntity<BaseApiResponse<List<DietRoadmapDTO>>> getHistory(Authentication authentication) {
        String username = authentication.getName();
        List<DietRoadmapDTO> result = dietRoadmapService.getHistory(username);
        return ResponseEntity.ok(BaseApiResponse.ok("Lấy lịch sử lộ trình thành công", result));
    }

    /**
     * DELETE /api/v1/diet-roadmap/{id}
     * Xóa lộ trình theo ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> delete(
            @PathVariable String id,
            Authentication authentication) {
        String username = authentication.getName();
        dietRoadmapService.delete(username, id);
        return ResponseEntity.ok(BaseApiResponse.ok("Đã xóa lộ trình thành công", null));
    }
}
