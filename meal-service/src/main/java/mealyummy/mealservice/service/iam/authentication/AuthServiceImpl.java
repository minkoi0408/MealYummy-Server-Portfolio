package mealyummy.mealservice.service.iam.authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.core.security.JwtUtil;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.enums.OtpType;
import mealyummy.mealservice.model.enums.TokenType;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.iam.authentication.dto.*;
import mealyummy.mealservice.service.iam.otp.OtpServiceImpl;
import mealyummy.mealservice.service.iam.token.TokenService;
import mealyummy.mealservice.service.iam.otp.TotpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final OtpServiceImpl otpService;
    private final TokenService tokenService;
    private final TotpService totpService;


    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;
    /**
     *
     * */
    @Override
    @Transactional
    public String registerLocal(RegisterRequestDTO request) {
        // Check existed username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Check existed email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Verify Otp
        boolean isOtpValid = otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.REGISTRATION);
        if (!isOtpValid) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // Set Role
        Role userRole = roleRepository.findByRoleCode("ROLE_FREE")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // Build User
        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail().trim().toLowerCase())
                .role(userRole)
                .build();

        log.info("Registering user: {}", request.getUsername());
        userRepository.save(user);
        return "Đăng ký thành công vui lòng đăng nhập !";
    }

    /**
     *
     * */
    public User getUserLogin(LoginRequestDTO request){
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Kiểm tra trạng thái tài khoản
        if (!user.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_INACTIVE);
        }
        return user;
    }

    /**
     * author Huynh Vu Minh Khoi
     * @since
     * */
    @Override
    public AuthResponseDTO login(VerifyLoginDTO request) {
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        log.info("Yêu cầu xác thực Email OTP cho user: {}", user.getUsername());

        boolean isOtpValid = otpService.verifyOtp(user.getEmail(), request.getOtp(), OtpType.LOGIN);
        if (!isOtpValid) {
            log.warn("Xác thực OTP thất bại hoặc hết hạn cho user: {}", user.getUsername());
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        Role userRole = user.getRole();
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), userRole.getRoleCode(), userRole.getPermissions());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        tokenService.storeToken(user.getUsername(), accessToken, TokenType.ACCESS.getPrefix(), jwtUtil.getAccessTokenExpirationMs());
        tokenService.storeToken(user.getUsername(), refreshToken, TokenType.REFRESH.getPrefix(), jwtUtil.getRefreshTokenExpirationMs());

        log.info("Đăng nhập thành công (đã qua MFA) cho user: {}", user.getUsername());

        return AuthResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     *
     * */
    @Override
    public void logout(String accessToken) {
        if (jwtUtil.validateToken(accessToken)) {
            String username = jwtUtil.getUsernameFromToken(accessToken);
            tokenService.removeAllTokensOfUser(username);
            log.info("Đăng xuất thành công cho user: {}", username);
        }
    }

    /**
     *
     * */
    @Override
    public String resetPassword(ResetPasswordDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.FORGOT_PASSWORD)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Đặt lại mật khẩu thành công cho user: {}", user.getUsername());
        return "Mật khẩu của bạn đã được thay đổi thành công.";
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

                Role userRole = roleRepository.findByRoleCode("ROLE_FREE")
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                User newUser = User.builder()
                        .username(preferredUsername)
                        .email(email)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(userRole)
                        .build();
                return userRepository.save(newUser);
            });

            // Tạo cặp token
            Role userRole = user.getRole();
            String accessToken = jwtUtil.generateAccessToken(user.getUsername(), userRole.getRoleCode(), userRole.getPermissions());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            tokenService.storeToken(user.getUsername(), accessToken, TokenType.ACCESS.getPrefix(), jwtUtil.getAccessTokenExpirationMs());
            tokenService.storeToken(user.getUsername(), refreshToken, TokenType.REFRESH.getPrefix(), jwtUtil.getRefreshTokenExpirationMs());

            log.info("Đăng nhập bằng Google thành công cho: {}", email);

            return AuthResponseDTO.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .avatarUrl(user.getAvatarUrl())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                .createdAt(user.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("Lỗi xác thực Google Token: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
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
    /**
     *
     * */
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

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Role userRole = user.getRole();
        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername(), userRole.getRoleCode(), userRole.getPermissions());
        tokenService.storeToken(username, newAccessToken, TokenType.ACCESS.getPrefix(), jwtUtil.getAccessTokenExpirationMs());

        log.info("Refresh token thành công cho user: {}", username);

        return AuthResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .createdAt(user.getCreatedAt()) // Giữ nguyên refresh token cũ
                .build();
    }
}
