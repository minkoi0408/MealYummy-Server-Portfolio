package mealyummy.mealservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.auth.AuthService;
import mealyummy.mealservice.service.auth.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Đăng ký tài khoản mới
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<BaseApiResponse<String>> register(
            @Valid @RequestBody RegisterRequestDTO request) {

        String message = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok(message, null));
    }

    /**
     * Xác thực mã OTP sau khi đăng ký
     * POST /api/v1/auth/verify-otp
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<BaseApiResponse<String>> verifyOtp(
            @Valid @RequestBody OtpVerifyDTO request) {

        String message = authService.verifyOtp(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(message, null));
    }

    /**
     * Gửi lại mã OTP
     * POST /api/v1/auth/resend-otp?email=...
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<BaseApiResponse<String>> resendOtp(
            @RequestParam String email) {

        String message = authService.resendOtp(email);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(message, null));
    }

    /**
     * Đăng nhập - trả về Access Token (5 phút) + Refresh Token (1 tuần)
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Đăng nhập thành công", response));
    }

    /**
     * Làm mới Access Token bằng Refresh Token
     * POST /api/v1/auth/refresh
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> refreshToken(
            @RequestBody RefreshTokenRequestDTO request) {

        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Làm mới token thành công", response));
    }

    /**
     * Đăng xuất - Xóa tất cả token của user khỏi Redis
     * POST /api/v1/auth/logout
     * Header: Authorization: Bearer {accessToken}
     */
    @PostMapping("/logout")
    public ResponseEntity<BaseApiResponse<String>> logout(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Đăng xuất thành công", null));
    }
}
