package mealyummy.mealservice.service.iam.notification;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.enums.OtpType;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.iam.notification.dto.SendOtpToEmailRequestDTO;
import mealyummy.mealservice.service.iam.otp.OtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import static mealyummy.mealservice.service.iam.notification.EmailFormatUtil.sendOtpInForgotPassword;
import static mealyummy.mealservice.service.iam.notification.EmailFormatUtil.sendOtpInRegister;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    private final OtpService otpService;
    private final UserRepository userRepository;

    @Override
    public String sendOtpToEmail(SendOtpToEmailRequestDTO request, OtpType otpType, String subJect, String htmlContent) {
        String email = request.getEmail();
        String otp = otpService.generateAndSaveOtp(email, otpType);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject(subJect);

            helper.setText(htmlContent.formatted(otp, otpExpirationMinutes), true);
            mailSender.send(message);
            log.info("HTML OTP đã gửi thành công tới email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi gửi email HTML OTP tới {}: {}", email, e.getMessage());
            log.info("=== [DEV] OTP cho {}: {} ===", email, otp);
        }
        return "Đã gởi otp đến gmail. Vui lòng kiểm tra email để nhận otp!";
    }

    @Override
    public String sendOtpRegistration(SendOtpToEmailRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        sendOtpToEmail(
                request,
                OtpType.REGISTRATION,
                "MealYummy - Xác thực Bio-Fuel của bạn",
                sendOtpInRegister);
        return "Đã gởi otp đến gmail. Vui lòng kiểm tra email để nhận otp!";
    }

    @Override
    public String sendOtpForgotPassword(SendOtpToEmailRequestDTO request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        sendOtpToEmail(
                request,
                OtpType.FORGOT_PASSWORD,
                "MealYummy - Xác thực Quên mật khẩu tài khoản của bạn",
                sendOtpInForgotPassword);
        return "Đã gởi otp đến gmail. Vui lòng kiểm tra email để nhận otp!";
    }
}
