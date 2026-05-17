package mealyummy.mealservice.service.iam.permission;

import mealyummy.mealservice.service.iam.permission.dto.PermissionResponse;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> getAllPermissions();
}
