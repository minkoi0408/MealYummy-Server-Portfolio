package mealyummy.mealservice.service.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý OTP: tạo mã, lưu tạm, gửi email và xác thực
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final JavaMailSender mailSender;

    // Lưu tạm OTP trong bộ nhớ (key = email, value = OtpData)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    // Thời gian hết hạn OTP: 5 phút
    private static final long OTP_EXPIRATION_MS = 5 * 60 * 1000;

    /**
     * Tạo mã OTP 6 chữ số, lưu vào bộ nhớ và gửi email
     */
    public void generateAndSendOtp(String email) {
        String otp = generateOtp();
        otpStorage.put(email, new OtpData(otp, Instant.now().plusMillis(OTP_EXPIRATION_MS)));

        // Gửi OTP qua email
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("MealYummy - Mã xác thực OTP");
            message.setText("Chào bạn,\n\nMã OTP của bạn là: " + otp
                    + "\n\nMã này có hiệu lực trong 5 phút."
                    + "\nNếu bạn không yêu cầu mã này, vui lòng bỏ qua email này."
                    + "\n\n--- MealYummy Team ---");
            mailSender.send(message);
            log.info("OTP đã gửi thành công tới email: {}", email);
        } catch (Exception e) {
            log.error("Lỗi gửi email OTP tới {}: {}", email, e.getMessage());
            // Vẫn log OTP ra console để dev test
            log.info("=== [DEV] OTP cho {}: {} ===", email, otp);
        }
    }

    /**
     * Xác thực mã OTP
     * @return true nếu OTP đúng và chưa hết hạn
     */
    public boolean verifyOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);
        if (otpData == null) {
            return false;
        }

        // Kiểm tra hết hạn
        if (Instant.now().isAfter(otpData.expiresAt())) {
            otpStorage.remove(email);
            return false;
        }

        // Kiểm tra đúng mã
        if (otpData.code().equals(otp)) {
            otpStorage.remove(email); // Xóa sau khi dùng
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

    /**
     * Record lưu thông tin OTP
     */
    private record OtpData(String code, Instant expiresAt) {}
}
