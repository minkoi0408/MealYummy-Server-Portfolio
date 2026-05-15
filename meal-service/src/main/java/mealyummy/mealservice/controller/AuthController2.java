package mealyummy.mealservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.iam.authentication.AuthService2;
import mealyummy.mealservice.service.iam.authentication.dto.AuthResponseDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.LoginRequestDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.RegisterRequestDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.ResetPasswordDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController2 {
    private final AuthService2 authService;

    /**
     * author
     * @since
     * */
    @PostMapping("/register")
    public ResponseEntity<BaseApiResponse<String>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        String message = authService.registerLocal(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(message, null));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        String message = "Đăng nhập thành công";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(message, authService.login(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<BaseApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordDTO request) {
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
    }

    @GetMapping("/logout")
    public ResponseEntity<BaseApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Đăng xuất thành công", null));
    }
}
