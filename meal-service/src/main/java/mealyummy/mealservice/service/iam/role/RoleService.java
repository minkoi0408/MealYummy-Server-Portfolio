package mealyummy.mealservice.service.iam.role;

import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.service.iam.role.dto.RolePermissionDTO;
import mealyummy.mealservice.service.iam.role.dto.RoleResponse;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<RoleResponse> getAllRoles();
    RoleResponse getRole(String id);
    RoleResponse createRole(mealyummy.mealservice.service.iam.role.dto.RoleRequestDTO request);
    RoleResponse updateRole(String id, mealyummy.mealservice.service.iam.role.dto.RoleRequestDTO request);
    void deleteRole(String id);
    Role addPermissionsToRole(RolePermissionDTO request);
    Role deletePermissionsFromRole(RolePermissionDTO request);
}
