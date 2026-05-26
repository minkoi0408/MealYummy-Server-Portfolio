package mealyummy.mealservice.service.iam.role;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.repository.PermissionRepository;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.service.iam.role.dto.RolePermissionDTO;
import mealyummy.mealservice.service.iam.role.dto.RoleResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    @Override
    public List<RoleResponse> getAllRoles() {
        List<Role> roles = roleRepository.findAll();

        return roles.stream()
                .map(this::convertToResponse)
                .toList();
    }

    private RoleResponse convertToResponse(Role role) {
        return RoleResponse.builder()
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .permissions(role.getPermissions())
                .build();
    }

    @Override
    public RoleResponse getRole(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        return convertToResponse(role);
    }

    @Override
    public RoleResponse createRole(mealyummy.mealservice.service.iam.role.dto.RoleRequestDTO request) {
        if (roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
            throw new RuntimeException("RoleCode đã tồn tại");
        }

        Role role = Role.builder()
                .roleCode(request.getRoleCode())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .permissions(new HashSet<>())
                .build();

        roleRepository.save(role);
        return convertToResponse(role);
    }

    @Override
    public RoleResponse updateRole(String id, mealyummy.mealservice.service.iam.role.dto.RoleRequestDTO request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (!role.getRoleCode().equals(request.getRoleCode()) &&
                roleRepository.findByRoleCode(request.getRoleCode()).isPresent()) {
            throw new RuntimeException("RoleCode đã tồn tại");
        }

        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());

        roleRepository.save(role);
        return convertToResponse(role);
    }

    @Override
    public void deleteRole(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (role.getPermissions() != null && role.getPermissions().contains("ROLE_ASSIGN_PERMISSION")) {
            throw new AppException(ErrorCode.CANNOT_DELETE_ASSIGN_PERMISSION);
        }

        roleRepository.delete(role);
    }

    @Override
    public Role addPermissionsToRole(RolePermissionDTO request) {
        Role role = roleRepository.findByRoleCode(request.getRoleCode())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        long validCount = permissionRepository.countByPermissionCodeIn(request.getPermissionCodes());
        if (validCount != request.getPermissionCodes().size()) {
            throw new AppException(ErrorCode.PERMISSION_NOT_FOUND);
        }

        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }
        role.getPermissions().addAll(request.getPermissionCodes());

        return roleRepository.save(role);
    }

    @Override
    public Role deletePermissionsFromRole(RolePermissionDTO request) {
        if (request.getPermissionCodes() != null && request.getPermissionCodes().contains("ROLE_ASSIGN_PERMISSION")) {
            throw new AppException(ErrorCode.CANNOT_DELETE_ASSIGN_PERMISSION);
        }
        Role role = roleRepository.findByRoleCode(request.getRoleCode())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        if (CollectionUtils.isEmpty(role.getPermissions())) {
            throw new AppException(ErrorCode.ROLE_HAS_NO_PERMISSION);
        }

        boolean hasAllPermissionsToDelete = role.getPermissions().containsAll(request.getPermissionCodes());
        if (!hasAllPermissionsToDelete) {
            throw new AppException(ErrorCode.ROLE_HAS_NO_PERMISSION);
        }

        role.getPermissions().removeAll(request.getPermissionCodes());

        return roleRepository.save(role);
    }
}
