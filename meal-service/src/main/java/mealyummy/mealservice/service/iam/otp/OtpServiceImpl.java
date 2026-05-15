package mealyummy.mealservice.service.iam.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.enums.OtpType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

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
     *
     * */
    @Override
    public String generateAndSaveOtp(String email, OtpType type) {
        String otpCode = generateOtp();
        storeOtp(email, otpCode, type);
        return otpCode;
    }

    /**
     *
     * */
    @Override
    public boolean verifyOtp(String email, String otpCode, OtpType type) {
        String key = "OTP:" + type.name() + ":" + email;
        String storedOtp = redisTemplate.opsForValue().get(key);

        if (storedOtp != null && storedOtp.equals(otpCode)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
