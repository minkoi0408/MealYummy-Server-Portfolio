package mealyummy.mealservice.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.core.security.JwtUtil;
import mealyummy.mealservice.model.entity.User;
import mealyummy.mealservice.model.enums.UserStatus;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.auth.dto.*;
import mealyummy.mealservice.service.otp.OtpService;
import mealyummy.mealservice.service.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final TokenService tokenService;

    /**
     * Đăng ký tài khoản mới:
     * 1. Validate dữ liệu đầu vào
     * 2. Kiểm tra trùng username/email
     * 3. Hash mật khẩu và lưu user với status PENDING
     * 4. Gửi OTP qua email
     */
    @Override
    public String register(RegisterRequestDTO request) {
        // Kiểm tra mật khẩu xác nhận
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        // Kiểm tra trùng username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Kiểm tra trùng email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Tạo user mới với status PENDING (chờ xác thực OTP)
        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .status(UserStatus.PENDING)
                .build();

        userRepository.save(user);

        // Gửi OTP qua email
        otpService.generateAndSendOtp(user.getEmail());

        log.info("Đăng ký thành công cho user: {}, đang chờ xác thực OTP", user.getUsername());
        return "Đăng ký thành công! Vui lòng kiểm tra email để nhập mã OTP.";
    }

    /**
     * Xác thực OTP và kích hoạt tài khoản
     */
    @Override
    public String verifyOtp(OtpVerifyDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!isValid) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Kích hoạt tài khoản
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info("Xác thực OTP thành công cho user: {}", user.getUsername());
        return "Xác thực thành công! Bạn có thể đăng nhập ngay bây giờ.";
    }

    /**
     * Đăng nhập:
     * 1. Tìm user theo username
     * 2. Kiểm tra mật khẩu
     * 3. Kiểm tra trạng thái tài khoản
     * 4. Tạo Access Token + Refresh Token và lưu vào Redis
     */
    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        // Tìm user theo username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.ACCOUNT_BANNED);
        }

        // Tạo cặp token
        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Lưu vào Redis với TTL tương ứng
        tokenService.storeAccessToken(user.getUsername(), accessToken);
        tokenService.storeRefreshToken(user.getUsername(), refreshToken);

        log.info("Đăng nhập thành công cho user: {}", user.getUsername());

        return AuthResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Làm mới Access Token bằng Refresh Token:
     * 1. Validate Refresh Token (chữ ký JWT + tồn tại trong Redis)
     * 2. Tạo Access Token mới, lưu vào Redis
     * 3. Giữ nguyên Refresh Token cũ
     */
    @Override
    public AuthResponseDTO refreshToken(String refreshToken) {
        // Kiểm tra JWT hợp lệ
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // Kiểm tra đúng loại token
        String tokenType = jwtUtil.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);

        // Kiểm tra Refresh Token có tồn tại trong Redis không
        if (!tokenService.isRefreshTokenValid(username, refreshToken)) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // Tạo Access Token mới
        String newAccessToken = jwtUtil.generateAccessToken(username);
        tokenService.storeAccessToken(username, newAccessToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("Refresh token thành công cho user: {}", username);

        return AuthResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Giữ nguyên refresh token cũ
                .build();
    }

    /**
     * Đăng xuất: Xóa tất cả token của user khỏi Redis
     */
    @Override
    public void logout(String accessToken) {
        if (jwtUtil.validateToken(accessToken)) {
            String username = jwtUtil.getUsernameFromToken(accessToken);
            tokenService.removeAllTokensOfUser(username);
            log.info("Đăng xuất thành công cho user: {}", username);
        }
    }

    /**
     * Gửi lại mã OTP cho email
     */
    @Override
    public String resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            return "Tài khoản đã được xác thực rồi.";
        }

        otpService.generateAndSendOtp(email);

        log.info("Gửi lại OTP cho email: {}", email);
        return "Mã OTP mới đã được gửi tới email của bạn.";
    }
}
