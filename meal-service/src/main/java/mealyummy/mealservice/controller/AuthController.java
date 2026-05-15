//package mealyummy.mealservice.controller;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import mealyummy.mealservice.core.base.BaseApiResponse;
//import mealyummy.mealservice.service.iam.authentication.AuthService;
//import mealyummy.mealservice.service.iam.authentication.dto.*;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//
//    /**
//     * Đăng ký tài khoản mới
//     * POST /api/v1/auth/register
//     */
//    @PostMapping("/register")
//    public ResponseEntity<BaseApiResponse<String>> register(
//            @Valid @RequestBody RegisterRequestDTO request) {
//
//        String message = authService.register(request);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(BaseApiResponse.ok(message, null));
//    }
//
//    /**
//     * Xác thực mã OTP sau khi đăng ký
//     * POST /api/v1/auth/verify-otp
//     */
//    @PostMapping("/verify-otp")
//    public ResponseEntity<BaseApiResponse<String>> verifyOtp(
//            @Valid @RequestBody OtpVerifyDTO request) {
//
//        String message = authService.verifyOtp(request);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok(message, null));
//    }
//
//    /**
//     * Gửi lại mã OTP
//     * POST /api/v1/auth/resend-otp?email=...
//     */
//    @PostMapping("/resend-otp")
//    public ResponseEntity<BaseApiResponse<String>> resendOtp(
//            @RequestParam String email) {
//
//        String message = authService.resendOtp(email);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok(message, null));
//    }
//
//    /**
//     * Đăng nhập (MFA) - Yêu cầu xác thực OTP
//     * POST /api/v1/auth/login
//     */
//    @PostMapping("/login")
//    public ResponseEntity<BaseApiResponse<String>> login(
//            @Valid @RequestBody LoginRequestDTO request) {
//
//        String message = authService.login(request);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok(message, null));
//    }
//
//    /**
//     * Xác thực OTP (MFA) để nhận Token
//     * POST /api/v1/auth/verify-mfa-login
//     */
//    @PostMapping("/verify-mfa-login")
//    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> verifyMfaLogin(
//            @Valid @RequestBody VerifyLoginDTO request) {
//
//        AuthResponseDTO response = authService.verifyLoginMfa(request);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok("Đăng nhập thành công", response));
//    }
//
//    /**
//     * Gửi Email OTP cho màn hình MFA đăng nhập
//     */
//    @PostMapping("/send-login-otp")
//    public ResponseEntity<BaseApiResponse<String>> sendLoginOtp(
//            @RequestParam String email) {
//        String message = authService.sendLoginOtp(email);
//        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
//    }
//
//    /**
//     * Làm mới Access Token bằng Refresh Token
//     * POST /api/v1/auth/refresh
//     * Body: { "refreshToken": "..." }
//     */
//    @PostMapping("/refresh")
//    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> refreshToken(
//            @RequestBody RefreshTokenRequestDTO request) {
//
//        AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok("Làm mới token thành công", response));
//    }
//
//    /**
//     * Đăng xuất - Xóa tất cả token của user khỏi Redis
//     * POST /api/v1/auth/logout
//     * Header: Authorization: Bearer {accessToken}
//     */
//    @PostMapping("/logout")
//    public ResponseEntity<BaseApiResponse<String>> logout(
//            @RequestHeader("Authorization") String authHeader) {
//
//        String token = authHeader.replace("Bearer ", "");
//        authService.logout(token);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok("Đăng xuất thành công", null));
//    }
//
//    /**
//     * Đăng nhập bằng Google
//     * POST /api/v1/auth/google
//     */
//    @PostMapping("/google")
//    public ResponseEntity<BaseApiResponse<AuthResponseDTO>> loginWithGoogle(
//            @Valid @RequestBody GoogleTokenRequestDTO request) {
//
//        AuthResponseDTO response = authService.loginWithGoogle(request);
//
//        return ResponseEntity
//                .status(HttpStatus.OK)
//                .body(BaseApiResponse.ok("Đăng nhập bằng Google thành công", response));
//    }
//
//    /**
//     * Gửi OTP để đăng ký (trước khi tạo user)
//     */
//    @PostMapping("/send-otp-register")
//    public ResponseEntity<BaseApiResponse<String>> sendOtpRegister(
//            @RequestParam String email) {
//
//        String message = authService.sendOtpForRegistration(email);
//        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
//    }
//    /**
//     * Quên mật khẩu - Gửi OTP
//     */
//    @PostMapping("/forgot-password")
//    public ResponseEntity<BaseApiResponse<String>> forgotPassword(
//            @RequestParam String email) {
//        String message = authService.forgotPassword(email);
//        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
//    }
//
//    /**
//     * Đặt lại mật khẩu mới
//     */
//    @PostMapping("/reset-password")
//    public ResponseEntity<BaseApiResponse<String>> resetPassword(
//            @Valid @RequestBody ResetPasswordDTO request) {
//        String message = authService.resetPassword(request);
//        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
//    }
//    /**
//     * Cài đặt TOTP (Google Authenticator)
//     * Trả về URI hình ảnh QR code
//     */
//    @PostMapping("/setup-totp")
//    public ResponseEntity<BaseApiResponse<String>> setupTotp(
//            @RequestParam String username) throws Exception {
//        String qrCodeUri = authService.setupTotp(username);
//        return ResponseEntity.ok(BaseApiResponse.ok("Quét mã QR sau bằng Google Authenticator", qrCodeUri));
//    }
//
//    /**
//     * Xác thực mã TOTP đầu tiên để bật TOTP
//     */
//    @PostMapping("/verify-totp-setup")
//    public ResponseEntity<BaseApiResponse<String>> verifyTotpSetup(
//            @RequestParam String username,
//            @RequestParam String code) {
//        String message = authService.verifyAndEnableTotp(username, code);
//        return ResponseEntity.ok(BaseApiResponse.ok(message, null));
//    }
//}
