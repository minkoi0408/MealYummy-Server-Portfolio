package mealyummy.mealservice.service.iam.authentication;

import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.service.iam.authentication.dto.*;

public interface AuthService {
    String registerLocal(RegisterRequestDTO request);
    User getUserLogin(LoginRequestDTO request);
    AuthResponseDTO login(VerifyLoginDTO request);
    void logout(String accessToken);
    String resetPassword(ResetPasswordDTO request);
    AuthResponseDTO loginWithGoogle(GoogleTokenRequestDTO request);
    String setupTotp(String username) throws Exception;
    String verifyAndEnableTotp(String username, String code) ;
    AuthResponseDTO refreshToken(String refreshToken);
}
