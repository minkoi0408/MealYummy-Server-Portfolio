package mealyummy.mealservice.service.iam.authentication;

import mealyummy.mealservice.service.iam.authentication.dto.AuthResponseDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.LoginRequestDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.RegisterRequestDTO;
import mealyummy.mealservice.service.iam.authentication.dto2.ResetPasswordDTO;

public interface AuthService2 {
    String registerLocal(RegisterRequestDTO request);
    AuthResponseDTO login(LoginRequestDTO request);
    void logout(String accessToken);
    String resetPassword(ResetPasswordDTO request);
    AuthResponseDTO refreshToken(String refreshToken);
}
