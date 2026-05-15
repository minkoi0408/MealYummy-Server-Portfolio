package mealyummy.mealservice.service.iam.notification;

import mealyummy.mealservice.model.enums.OtpType;
import mealyummy.mealservice.service.iam.notification.dto.SendOtpToEmailRequestDTO;

public interface NotificationService {
    String sendOtpToEmail(SendOtpToEmailRequestDTO request, OtpType otpType, String subJect, String htmlContent);
    String sendOtpRegistration(SendOtpToEmailRequestDTO request);
    String sendOtpForgotPassword(SendOtpToEmailRequestDTO request);
}
