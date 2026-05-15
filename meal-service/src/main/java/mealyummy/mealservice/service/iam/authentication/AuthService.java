//package mealyummy.mealservice.service.iam.authentication;
//
//import mealyummy.mealservice.service.iam.authentication.dto.*;
//
//public interface AuthService {
//    String register(RegisterRequestDTO request);
//    String login(LoginRequestDTO request);
//    String sendLoginOtp(String email);
//    AuthResponseDTO verifyLoginMfa(VerifyLoginDTO request);
//    String verifyOtp(OtpVerifyDTO request);
//    String resendOtp(String email);
//    AuthResponseDTO refreshToken(String refreshToken);
//    AuthResponseDTO loginWithGoogle(GoogleTokenRequestDTO request);
//    String sendOtpForRegistration(String email);
//    void logout(String accessToken);
//    String forgotPassword(String email);
//    String resetPassword(ResetPasswordDTO request);
//    String setupTotp(String username) throws Exception;
//    String verifyAndEnableTotp(String username, String code);
//}
