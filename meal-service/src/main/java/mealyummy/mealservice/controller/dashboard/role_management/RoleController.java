package mealyummy.mealservice.controller.dashboard.role_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.service.iam.role.RoleService;
import mealyummy.mealservice.service.iam.role.dto.RolePermissionDTO;
import mealyummy.mealservice.service.iam.role.dto.RoleRequestDTO;
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

    @PreAuthorize("hasAuthority('VIEW_ALL_ROLE')")
    @GetMapping
    public ResponseEntity<BaseApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách vai trò thành công", roles));
    }

    @PreAuthorize("hasAuthority('VIEW_ROLE')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<RoleResponse>> getRole(@PathVariable String id) {
        RoleResponse role = roleService.getRole(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thông tin vai trò", role));
    }

    @PreAuthorize("hasAuthority('CREATE_ROLE')")
    @PostMapping
    public ResponseEntity<BaseApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequestDTO request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok("Tạo mới vai trò thành công", role));
    }

    @PreAuthorize("hasAuthority('UPDATE_ROLE')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<RoleResponse>> updateRole(
            @PathVariable String id,
            @Valid @RequestBody RoleRequestDTO request) {
        RoleResponse role = roleService.updateRole(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật vai trò thành công", role));
    }

    @PreAuthorize("hasAuthority('DELETE_ROLE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa vai trò thành công", null));
    }

    @PreAuthorize("hasAuthority('ROLE_ASSIGN_PERMISSION')")
    @PutMapping("/permissions/add")
    public ResponseEntity<BaseApiResponse<Role>> addPermissionsToRole(
            @Valid @RequestBody RolePermissionDTO request) {
        Role updatedRole = roleService.addPermissionsToRole(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật thêm quyền cho vai trò thành công", updatedRole));
    }

    @PreAuthorize("hasAuthority('ROLE_REVOKE_PERMISSION')")
    @PutMapping("/permissions/delete")
    public ResponseEntity<BaseApiResponse<Role>> deletePermissionsFromRole(
            @Valid @RequestBody RolePermissionDTO request) {
        Role updatedRole = roleService.deletePermissionsFromRole(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa quyền khỏi vai trò thành công", updatedRole));
    }
}