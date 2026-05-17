package mealyummy.mealservice.service.iam.notification;

import mealyummy.mealservice.model.enums.OtpType;
import mealyummy.mealservice.service.iam.authentication.dto.LoginRequestDTO;
import mealyummy.mealservice.service.iam.notification.dto.SendOtpToEmailRequestDTO;

public interface NotificationService {
    void sendOtpToEmail(String email, OtpType otpType, String subJect, String htmlContent);
    String sendOtpRegistration(SendOtpToEmailRequestDTO request);
    String sendOtpForgotPassword(SendOtpToEmailRequestDTO request);
    String sendOtpLogin(LoginRequestDTO request);
}
