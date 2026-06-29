package mealyummy.mealservice.controller.dashboard;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.iam.token.TokenServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard/active-users")
@RequiredArgsConstructor
public class ActiveUserController {

    private final TokenServiceImpl tokenService;

    @PreAuthorize("@apiAuth.check()")
    @GetMapping
    public ResponseEntity<BaseApiResponse<List<String>>> getActiveUsers() {
        List<String> activeUsers = tokenService.getActiveUsers();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lay danh sach nguoi dung dang online thanh cong", activeUsers));
    }
}
