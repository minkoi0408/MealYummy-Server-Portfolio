package mealyummy.mealservice.service.iam.authentication.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyLoginDTO {

    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    private String username;

    @NotBlank(message = "Mã OTP không được để trống")
    private String otp;
}
