package mealyummy.mealservice.service.iam.permission;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.auth.Permission;
import mealyummy.mealservice.model.repository.PermissionRepository;
import mealyummy.mealservice.service.iam.permission.dto.PermissionResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;
    @Override
    public List<PermissionResponse> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream()
                .map(permission -> PermissionResponse.builder()
                        .permissionCode(permission.getPermissionCode())
                        .permissionName(permission.getPermissionName())
                        .type(permission.getType())
                        .description(permission.getDescription())
                        .build())
                .toList();
    }
}
