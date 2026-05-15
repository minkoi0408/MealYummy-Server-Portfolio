package mealyummy.mealservice.service.iam.authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.core.security.JwtUtil;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.enums.AuthProvider;
import mealyummy.mealservice.model.enums.OtpType;
import mealyummy.mealservice.model.enums.TokenType;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.iam.authentication.dto.AuthResponseDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.LoginRequestDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.RegisterRequestDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.ResetPasswordDTO;
import mealyummy.mealservice.service.iam.otp.OtpServiceImpl;
import mealyummy.mealservice.service.iam.token.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl2 implements AuthService2 {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final OtpServiceImpl otpService;
    private final TokenService tokenService;

    /**
     * author Nguyen Nhut Huy
     * @since May 15th 2026
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
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUDN));

        // Build User
        User user = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail().trim().toLowerCase())
                .role(userRole)
                .authProvider(AuthProvider.LOCAL)
                .build();

        log.info("Registering user: {}", request.getUsername());
        userRepository.save(user);
        return "Đăng ký thành công vui lòng đăng nhập !";
    }

    /**
     * author Huynh Vu Minh Khoi
     * @since
     * */
    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
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

        log.info("Yêu cầu xác thực Email OTP cho user: {}", user.getUsername());

        // Tạo cặp token
        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Lưu vào Redis với TTL tương ứng
        tokenService.storeToken(user.getUsername(), accessToken, TokenType.ACCESS.getPrefix(), jwtUtil.getAccessTokenExpirationMs());
        tokenService.storeToken(user.getUsername(), refreshToken, TokenType.REFRESH.getPrefix(), jwtUtil.getAccessTokenExpirationMs());

        log.info("Đăng nhập thành công (đã qua MFA) cho user: {}", user.getUsername());

        return AuthResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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

    /***/
    @Override
    public AuthResponseDTO refreshToken(String refreshToken) {
        return null;
    }
}
