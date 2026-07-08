package mealyummy.mealservice.service.iam.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.enums.OtpType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {
    @Value("${app.otp.expiration-minutes}")
    private long otpExpirationMinutes;

    private final StringRedisTemplate redisTemplate;
    /**
     *
     * */
    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     *
     * */
    public void storeOtp(String email, String otpCode, OtpType type) {
        String key = "OTP:" + type.name() + ":" + email;
        redisTemplate.opsForValue().set(
                key,
                otpCode,
                otpExpirationMinutes,
                TimeUnit.MINUTES
        );

        log.info("Đã lưu OTP loại {} cho: {}. Key: {}", type, email, key);
    }

    /**
     * Tạo và lưu OTP mới. Chỉ rate-limit khi RESEND (đã có OTP tồn tại trước đó).
     * Lần đầu gửi (đăng nhập, đăng ký) không bị giới hạn.
     */
    @Override
    public String generateAndSaveOtp(String email, OtpType type) {
        String otpKey = "OTP:" + type.name() + ":" + email;
        boolean isResend = Boolean.TRUE.equals(redisTemplate.hasKey(otpKey));

        // Chỉ kiểm tra giới hạn khi là RESEND (spam bấm "Gửi lại mã")
        if (isResend) {
            String clientIp = getClientIp();
            String dailyIpKey = "OTP_RESEND_IP:" + clientIp;
            String dailyEmailKey = "OTP_RESEND_EMAIL:" + email;

            // Kiểm tra giới hạn 5 lần resend/ngày theo IP
            String ipCountStr = redisTemplate.opsForValue().get(dailyIpKey);
            int ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;
            if (ipCount >= 5 && !"UNKNOWN".equals(clientIp)) {
                throw new AppException(ErrorCode.OTP_DAILY_LIMIT_EXCEEDED);
            }

            // Kiểm tra giới hạn 5 lần resend/ngày theo Email
            String emailCountStr = redisTemplate.opsForValue().get(dailyEmailKey);
            int emailCount = emailCountStr != null ? Integer.parseInt(emailCountStr) : 0;
            if (emailCount >= 5) {
                throw new AppException(ErrorCode.OTP_DAILY_LIMIT_EXCEEDED);
            }

            // Tăng bộ đếm resend
            if (ipCount == 0 && !"UNKNOWN".equals(clientIp)) {
                redisTemplate.opsForValue().set(dailyIpKey, "1", 1, TimeUnit.DAYS);
            } else if (!"UNKNOWN".equals(clientIp)) {
                redisTemplate.opsForValue().increment(dailyIpKey);
            }
            if (emailCount == 0) {
                redisTemplate.opsForValue().set(dailyEmailKey, "1", 1, TimeUnit.DAYS);
            } else {
                redisTemplate.opsForValue().increment(dailyEmailKey);
            }
        }

        // Reset bộ đếm nhập sai khi gửi mã mới
        redisTemplate.delete("OTP_ATTEMPTS:" + type.name() + ":" + email);

        String otpCode = generateOtp();
        storeOtp(email, otpCode, type);
        return otpCode;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attribs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attribs != null) {
                HttpServletRequest request = attribs.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null) {
                    return request.getRemoteAddr();
                }
                return xfHeader.split(",")[0].trim();
            }
        } catch (Exception e) {}
        return "UNKNOWN";
    }

    /**
     *
     * */
    @Override
    public boolean verifyOtp(String email, String otpCode, OtpType type) {
        String key = "OTP:" + type.name() + ":" + email;
        String attemptKey = "OTP_ATTEMPTS:" + type.name() + ":" + email;

        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp == null) {
            return false;
        }

        String attemptsStr = redisTemplate.opsForValue().get(attemptKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= 5) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
            throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }

        if (storedOtp.equals(otpCode)) {
            redisTemplate.delete(key);
            redisTemplate.delete(attemptKey);
            return true;
        }

        redisTemplate.opsForValue().increment(attemptKey);
        redisTemplate.expire(attemptKey, otpExpirationMinutes, TimeUnit.MINUTES);
        
        return false;
    }
}
