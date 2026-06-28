package mealyummy.mealservice.service.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Service quản lý OTP sử dụng Redis để lưu trữ tạm thời
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    // Lấy thời gian hết hạn OTP từ env
    @org.springframework.beans.factory.annotation.Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;
    /**
     * Gửi mã OTP qua email với định dạng HTML
     */
    public void sendOtpEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("🛡️ MealYummy - Xác thực Bio-Fuel của bạn");

            String htmlContent = """
                <div style="background-color: #0b1006; color: #e0e4d2; font-family: 'Lexend', sans-serif; padding: 40px; border-radius: 16px; border: 1px solid #32362a; max-width: 600px; margin: 0 auto;">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <h1 style="color: #ccff80; font-size: 32px; font-weight: 900; font-style: italic; margin: 0; text-shadow: 0 0 10px rgba(163,230,53,0.3);">MealYummy</h1>
                        <p style="color: #8c947c; font-size: 12px; letter-spacing: 2px; text-transform: uppercase; margin-top: 5px;">High-Performance Bio-Fuel System</p>
                    </div>
                    
                    <div style="background: rgba(26, 31, 46, 0.4); padding: 30px; border-radius: 12px; border: 1px solid rgba(255, 255, 255, 0.1); text-align: center;">
                        <h2 style="color: #ffffff; margin-bottom: 20px;">Xác thực tài khoản</h2>
                        <p style="color: #c2cab0; line-height: 1.6;">Để tiếp tục hành trình tối ưu hóa cơ thể, vui lòng sử dụng mã xác thực dưới đây:</p>
                        
                        <div style="background-color: #1d2116; color: #ccff80; font-size: 48px; font-weight: bold; letter-spacing: 10px; padding: 20px; border-radius: 8px; margin: 30px 0; border: 1px dashed #424936;">
                            %s
                        </div>
                        
                        <p style="color: #8c947c; font-size: 13px;">Mã xác thực có hiệu lực trong %d phút.</p>
                    </div>
                    
                    <div style="margin-top: 30px; text-align: center; border-top: 1px solid #32362a; padding-top: 20px;">
                        <p style="color: #c2cab0; font-size: 14px; font-style: italic;">"Your health is the ultimate performance."</p>
                        <p style="color: #424936; font-size: 11px; margin-top: 20px;">
                            © 2024 MealYummy Inc. | Hệ thống dinh dưỡng cá nhân hóa AI.<br/>
                            Nếu bạn không yêu cầu mã này, vui lòng bỏ qua.
                        </p>
                    </div>
                </div>
            """.formatted(otp, otpExpirationMinutes);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML OTP đã gửi thành công tới email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi gửi email HTML OTP tới {}: {}", email, e.getMessage());
            log.info("=== [DEV] OTP cho {}: {} ===", email, otp);
        }
    }

    /**
     * Tạo mã OTP 6 chữ số, lưu vào Redis và gửi email
     */
    public void generateAndSendOtp(String email) {
        String otp = generateOtp();
        redisTemplate.opsForValue().set(
                "otp:" + email, 
                otp, 
                Duration.ofMinutes(otpExpirationMinutes)
        );
        sendOtpEmail(email, otp);
    }

    /**
     * Xác thực mã OTP từ Redis
     */
    public boolean verifyOtp(String email, String otp) {
        String key = "otp:" + email;
        String savedOtp = redisTemplate.opsForValue().get(key);

        if (savedOtp != null && savedOtp.equals(otp)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    /**
     * Tạo mã OTP ngẫu nhiên 6 chữ số
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
