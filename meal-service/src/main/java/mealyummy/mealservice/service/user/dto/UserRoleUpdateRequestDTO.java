package mealyummy.mealservice.service.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRoleUpdateRequestDTO {
    @NotBlank(message = "Role ID không được để trống")
    private String roleId;
}
