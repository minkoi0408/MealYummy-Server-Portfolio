package mealyummy.mealservice.service.iam.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.enums.OtpType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpToEmailRequestDTO {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private OtpType otpType;
}
