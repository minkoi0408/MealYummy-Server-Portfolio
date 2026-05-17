package mealyummy.mealservice.service.iam.role.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleResponse {
    private String roleCode;
    private String roleName;
    private String description;
    private Set<String> permissions;

}
