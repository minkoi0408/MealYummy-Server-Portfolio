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

    @PreAuthorize("@apiAuth.check()")
    @GetMapping
    public ResponseEntity<BaseApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách vai trò thành công", roles));
    }

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<RoleResponse>> getRole(@PathVariable String id) {
        RoleResponse role = roleService.getRole(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thông tin vai trò", role));
    }

    @PreAuthorize("@apiAuth.check()")
    @PostMapping
    public ResponseEntity<BaseApiResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequestDTO request) {
        RoleResponse role = roleService.createRole(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseApiResponse.ok("Tạo mới vai trò thành công", role));
    }

    @PreAuthorize("@apiAuth.check()")
    @PutMapping("/{id}")
    public ResponseEntity<BaseApiResponse<RoleResponse>> updateRole(
            @PathVariable String id,
            @Valid @RequestBody RoleRequestDTO request) {
        RoleResponse role = roleService.updateRole(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật vai trò thành công", role));
    }

    @PreAuthorize("@apiAuth.check()")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseApiResponse<Void>> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa vai trò thành công", null));
    }

    @PreAuthorize("@apiAuth.check()")
    @PutMapping("/permissions/add")
    public ResponseEntity<BaseApiResponse<Role>> addPermissionsToRole(
            @Valid @RequestBody RolePermissionDTO request) {
        Role updatedRole = roleService.addPermissionsToRole(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật thêm quyền cho vai trò thành công", updatedRole));
    }

    @PreAuthorize("@apiAuth.check()")
    @PutMapping("/permissions/delete")
    public ResponseEntity<BaseApiResponse<Role>> deletePermissionsFromRole(
            @Valid @RequestBody RolePermissionDTO request) {
        Role updatedRole = roleService.deletePermissionsFromRole(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Xóa quyền khỏi vai trò thành công", updatedRole));
    }
}