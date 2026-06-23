package mealyummy.mealservice.controller.dashboard.user_management;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.user.UserService;
import mealyummy.mealservice.service.user.dto.UserResponseDTO;
import mealyummy.mealservice.service.user.dto.UserRoleUpdateRequestDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;

    @PreAuthorize("@apiAuth.check()")
    @GetMapping
    public ResponseEntity<BaseApiResponse<Page<UserResponseDTO>>> getAllUsers(Pageable pageable) {
        Page<UserResponseDTO> response = userService.getAllUsers(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lấy danh sách người dùng thành công", response));
    }

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/{id}")
    public ResponseEntity<BaseApiResponse<UserResponseDTO>> getUser(@PathVariable String id) {
        UserResponseDTO response = userService.getUser(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thông tin người dùng", response));
    }

    @PreAuthorize("@apiAuth.check()")
    @PutMapping("/{id}/role")
    public ResponseEntity<BaseApiResponse<UserResponseDTO>> updateUserRole(
            @PathVariable String id,
            @Valid @RequestBody UserRoleUpdateRequestDTO request) {
        UserResponseDTO response = userService.updateUserRole(id, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cập nhật vai trò người dùng thành công", response));
    }
}
