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
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.UUID;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final TokenService tokenService;
    private final TotpService totpService;

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    @Value("${app.otp.expiration-minutes:1}")
    private long otpExpirationMinutes;

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

        // Nếu có OTP truyền vào -> Xác thực ngay
        if (request.getOtp() != null && !request.getOtp().isBlank()) {
            boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
            if (!isValid) {
                throw new AppException(ErrorCode.OTP_INVALID);
            }
            
            // Nếu OTP đúng -> Tạo user ACTIVE luôn
            User user = User.builder()
                    .username(request.getUsername().trim())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .email(request.getEmail().trim().toLowerCase())
                    .phone(request.getPhone().trim())
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            return "Đăng ký thành công!";
        }

        // Nếu không có OTP (luồng cũ hoặc người dùng nhấn Đăng ký mà chưa nhập OTP)
        // Tạo user PENDING
        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .status(UserStatus.PENDING)
                .build();
        
        String otp = generateRandomOtp();
        user.setOtpCode(otp);
        user.setOtpExpiration(Instant.now().plusSeconds(otpExpirationMinutes * 60));
        userRepository.save(user);
        otpService.sendOtpEmail(user.getEmail(), otp);

        return "Đăng ký thành công! Vui lòng kiểm tra email để nhập mã OTP.";
    }

    /**
     * Xác thực OTP và kích hoạt tài khoản
     */
    @Override
    public String verifyOtp(OtpVerifyDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra OTP trong Database
        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Kiểm tra hết hạn
        if (user.getOtpExpiration() != null && Instant.now().isAfter(user.getOtpExpiration())) {
            throw new AppException(ErrorCode.OTP_INVALID); // Hoặc OTP_EXPIRED nếu có ErrorCode này
        }

        // Kích hoạt tài khoản
        user.setStatus(UserStatus.ACTIVE);
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        log.info("Xác thực OTP thành công cho user: {}", user.getUsername());
        return "Xác thực thành công! Bạn có thể đăng nhập ngay bây giờ.";
    }

    @Override
    public String login(LoginRequestDTO request) {
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
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

        // Luôn luôn gửi Email OTP làm mặc định
        otpService.generateAndSendOtp(user.getEmail());
        log.info("Yêu cầu xác thực Email OTP cho user: {}", user.getUsername());

        // Kiểm tra xem User có bật Google Authenticator không để báo cho Frontend
        if (user.isTotpEnabled()) {
            return "Vui lòng kiểm tra email để nhận mã OTP.|TOTP_ENABLED";
        }

        return "Vui lòng kiểm tra email để nhận mã OTP đăng nhập.";
    }

    @Override
    public String sendLoginOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        otpService.generateAndSendOtp(user.getEmail());
        log.info("Yêu cầu xác thực Email OTP (thủ công) cho user: {}", user.getUsername());
        return "Mã OTP đã được gửi tới email của bạn.";
    }

    @Override
    public AuthResponseDTO verifyLoginMfa(VerifyLoginDTO request) {
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        boolean isValid = false;

        // Kiểm tra xem User dùng TOTP hay Email OTP
        if (user.isTotpEnabled()) {
            if (totpService.verifyCode(user.getTotpSecret(), request.getOtp())) {
                isValid = true;
            } else if (otpService.verifyOtp(user.getEmail(), request.getOtp())) {
                // Cho phép fallback về Email OTP nếu nhập đúng
                isValid = true;
            }
        } else {
            // Kiểm tra OTP từ Redis
            if (otpService.verifyOtp(user.getEmail(), request.getOtp())) {
                isValid = true;
            }
        }

        if (!isValid) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Tạo cặp token
        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Lưu vào Redis với TTL tương ứng
        tokenService.storeAccessToken(user.getUsername(), accessToken);
        tokenService.storeRefreshToken(user.getUsername(), refreshToken);

        log.info("Đăng nhập thành công (đã qua MFA) cho user: {}", user.getUsername());

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

        String otp = generateRandomOtp();
        user.setOtpCode(otp);
        user.setOtpExpiration(Instant.now().plusSeconds(otpExpirationMinutes * 60));
        userRepository.save(user);

        otpService.sendOtpEmail(email, otp);

        log.info("Gửi lại OTP cho email: {}", email);
        return "Mã OTP mới đã được gửi tới email của bạn.";
    }

    /**
     * Đăng nhập bằng Google:
     * 1. Xác thực ID Token gửi từ Frontend
     * 2. Lấy thông tin email, name từ Google
     * 3. Nếu user chưa tồn tại -> tạo mới (ACTIVE)
     * 4. Tạo cặp token JWT
     */
    @Override
    public AuthResponseDTO loginWithGoogle(GoogleTokenRequestDTO request) {
        log.info("Bắt đầu xác thực Google Token. Google Client ID cấu hình: {}", googleClientId);
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getToken());
            if (idToken == null) {
                log.error("Xác thực Google ID Token thất bại: idToken is null. Hãy kiểm tra Google Client ID và thời gian hệ thống.");
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Tìm hoặc tạo user mới
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                // Ưu tiên lấy tên từ Google làm Username cho đẹp
                String preferredUsername = (name != null && !name.isBlank()) ? name : email;
                
                User newUser = User.builder()
                        .username(preferredUsername) 
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .phone("")
                        .status(UserStatus.ACTIVE)
                        .build();
                return userRepository.save(newUser);
            });

            // Tạo cặp token
            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            tokenService.storeAccessToken(user.getUsername(), accessToken);
            tokenService.storeRefreshToken(user.getUsername(), refreshToken);

            log.info("Đăng nhập bằng Google thành công cho: {}", email);

            return AuthResponseDTO.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi xác thực Google Token: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public String sendOtpForRegistration(String email) {
        // Kiểm tra email tồn tại chưa
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        
        otpService.generateAndSendOtp(email);
        return "Mã OTP đã được gửi tới email của bạn.";
    }

    @Override
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isTotpEnabled()) {
            return "Vui lòng nhập mã Google Authenticator để đặt lại mật khẩu.";
        }

        // Gửi OTP qua OtpService (Lưu vào Redis)
        otpService.generateAndSendOtp(email);
        log.info("Gửi OTP quên mật khẩu vào Redis cho email: {}", email);
        return "Mã xác thực đã được gửi tới email của bạn.";
    }

    @Override
    public String resetPassword(ResetPasswordDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isTotpEnabled()) {
            if (!totpService.verifyCode(user.getTotpSecret(), request.getOtp())) {
                throw new AppException(ErrorCode.OTP_INVALID);
            }
        } else {
            // Kiểm tra OTP từ Redis
            if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
                throw new AppException(ErrorCode.OTP_INVALID);
            }
        }

        // Đổi mật khẩu
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Đặt lại mật khẩu thành công cho user: {}", user.getUsername());
        return "Mật khẩu của bạn đã được thay đổi thành công.";
    }

    @Override
    public String setupTotp(String username) throws Exception {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        return totpService.getQrCodeImageUri(secret, user.getEmail());
    }

    @Override
    public String verifyAndEnableTotp(String username, String code) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getTotpSecret() == null) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        user.setTotpEnabled(true);
        userRepository.save(user);

        return "Google Authenticator đã được kích hoạt thành công!";
    }

    private String generateRandomOtp() {
        return String.valueOf(100000 + new java.security.SecureRandom().nextInt(900000));
    }
}
