package mealyummy.mealservice.service.iam.otp;

import mealyummy.mealservice.model.enums.OtpType;

public interface OtpService {
    String generateOtp();
    void storeOtp(String identifier, String otpCode, OtpType type);
    String generateAndSaveOtp(String email, OtpType type);
    boolean verifyOtp(String email, String otpCode, OtpType type);
}
