package mealyummy.mealservice.service.iam.permission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {
    private String permissionCode;
    private String permissionName;
    private String type;
    private String description;
}