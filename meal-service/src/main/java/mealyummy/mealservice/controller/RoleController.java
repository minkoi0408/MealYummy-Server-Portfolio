package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.service.iam.role.RoleService;
import mealyummy.mealservice.service.iam.role.dto.RolePermissionDTO;
import mealyummy.mealservice.service.iam.role.dto.RoleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<List<RoleResponse>>> getAllRoles() {

        List<RoleResponse> roles = roleService.getAllRoles();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách vai trò thành công", roles));
    }
    @PreAuthorize("hasAuthority('ROLE_ASSIGN_PERMISSION')")
    @PutMapping("/permissions/add")
    public ResponseEntity<BaseApiResponse<Role>> addPermissionsToRole(
            @RequestBody RolePermissionDTO request) {

        Role updatedRole = roleService.addPermissionsToRole(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật thêm quyền cho vai trò thành công", updatedRole));
    }

    @PreAuthorize("hasAuthority('ROLE_REVOKE_PERMISSION')")
    @PutMapping("/permissions/delete")
    public ResponseEntity<BaseApiResponse<Role>> deletePermissionsFromRole(
            @RequestBody RolePermissionDTO request) {

        Role updatedRole = roleService.deletePermissionsFromRole(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa quyền khỏi vai trò thành công", updatedRole));
    }
}