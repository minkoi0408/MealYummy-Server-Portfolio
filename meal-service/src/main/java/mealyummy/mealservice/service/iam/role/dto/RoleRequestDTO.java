package mealyummy.mealservice.service.iam.role.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleRequestDTO {
    @NotBlank(message = "Mã vai trò không được để trống")
    private String roleCode;

    @NotBlank(message = "Tên vai trò không được để trống")
    private String roleName;

    private String description;
}
