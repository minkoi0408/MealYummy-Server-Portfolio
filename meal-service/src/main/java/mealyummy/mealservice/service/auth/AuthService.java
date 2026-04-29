package mealyummy.mealservice.service.auth;

import mealyummy.mealservice.service.auth.dto.*;

public interface AuthService {
    String register(RegisterRequestDTO request);
    AuthResponseDTO login(LoginRequestDTO request);
    String verifyOtp(OtpVerifyDTO request);
    String resendOtp(String email);
    AuthResponseDTO refreshToken(String refreshToken);
    void logout(String accessToken);
}
