package mealyummy.mealservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.iam.authentication.AuthService;
import mealyummy.mealservice.service.iam.authentication.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

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
            @Valid @RequestBody VerifyLoginDTO request) {

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

    @PostMapping("/refresh")
    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> refreshToken(
            @RequestBody RefreshTokenRequestDTO request) {

        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Làm mới token thành công", response));
    }

    /**
     * Đăng nhập bằng Google
     * POST /api/v1/auth/google
     */
    @PostMapping("/google")
    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> loginWithGoogle(
            @Valid @RequestBody GoogleTokenRequestDTO request) {

        AuthResponseDTO response = authService.loginWithGoogle(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Đăng nhập bằng Google thành công", response));
    }

    /**
     * Cài đặt TOTP (Google Authenticator)
     * Trả về URI hình ảnh QR code
     */
    @PostMapping("/setup-totp")
    public ResponseEntity<BaseApiResponse<String>> setupTotp(
            @RequestParam String username) throws Exception {
        String qrCodeUri = authService.setupTotp(username);
        return ResponseEntity.ok(BaseApiResponse.ok("Quét mã QR sau bằng Google Authenticator", qrCodeUri));
    }

    /**
     * Xác thực mã TOTP đầu tiên để bật TOTP
     */
    @PostMapping("/verify-totp-setup")
    public ResponseEntity<BaseApiResponse<String>> verifyTotpSetup(
            @RequestParam String username,
            @RequestParam String code) {
        String message = authService.verifyAndEnableTotp(username, code);
        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
    }
}
