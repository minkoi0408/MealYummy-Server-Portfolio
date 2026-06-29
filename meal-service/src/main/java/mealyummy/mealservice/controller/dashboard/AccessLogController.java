package mealyummy.mealservice.controller.dashboard;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.entity.log.AccessLog;
import mealyummy.mealservice.service.log.AccessLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard/access-logs")
@RequiredArgsConstructor
public class AccessLogController {

    private final AccessLogService accessLogService;

    @PreAuthorize("@apiAuth.check()")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<AccessLog>>> getAccessLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        
        Page<AccessLog> logs = accessLogService.getLogs(page, size, search);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lay danh sach access logs thanh cong", logs));
    }
}