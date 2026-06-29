package mealyummy.mealservice.controller.dashboard;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.analytics.AnalyticsService;
import mealyummy.mealservice.service.analytics.dto.DashboardOverviewDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/overview")
    public ResponseEntity<BaseApiResponse<DashboardOverviewDTO>> getOverview() {
        DashboardOverviewDTO data = analyticsService.getDashboardOverview();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Get overview successfully", data));
    }
}
