package mealyummy.mealservice.service.iam.role.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionDTO {
    private String roleCode;
    private Set<String> permissionCodes;
}
