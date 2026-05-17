package mealyummy.mealservice.core.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.entity.auth.Permission;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.repository.PermissionRepository;
import mealyummy.mealservice.model.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
        initPermission();
    }

    /**
     * Hàm khởi tạo 4 Role cơ bản: Free, Membership, Admin, Manager.
     * Permissions được để trống mặc định.
     */
    private void initRoles() {
        // Chỉ thêm dữ liệu nếu collection 'roles' chưa có bản ghi nào
        if (roleRepository.count() == 0) {
            log.info("--- Bắt đầu khởi tạo dữ liệu Role mặc định ---");

            Role freeRole = Role.builder()
                    .roleCode("ROLE_FREE")
                    .roleName("Free")
                    .description("Tài khoản người dùng miễn phí mặc định")
                    .build();

            Role membershipRole = Role.builder()
                    .roleCode("ROLE_MEMBERSHIP")
                    .roleName("Membership")
                    .description("Tài khoản người dùng đã đăng ký gói trả phí")
                    .build();

            Role managerRole = Role.builder()
                    .roleCode("ROLE_MANAGER")
                    .roleName("Manager")
                    .description("Quản lý nội dung, món ăn và thực đơn")
                    .build();

            Role adminRole = Role.builder()
                    .roleCode("ROLE_ADMIN")
                    .roleName("Admin")
                    .description("Quản trị viên tối cao của hệ thống")
                    .build();

            roleRepository.saveAll(List.of(freeRole, membershipRole, managerRole, adminRole));

            log.info("--- Đã khởi tạo thành công 4 Roles: FREE, MEMBERSHIP, MANAGER, ADMIN ---");
        } else {
            log.info("--- Dữ liệu Role đã tồn tại, bỏ qua bước khởi tạo ---");
        }
    }

private void initPermission() {
    if (permissionRepository.count() > 0) {
        return;
    }

    // Tạo danh sách các permission bằng Builder pattern mẫu
    List<Permission> permissions = List.of(
            Permission.builder()
                    .permissionCode("ROLE_VIEW")
                    .permissionName("Xem danh sách vai trò")
                    .type("ROLE")
                    .description("Cho phép xem thông tin cấu hình và danh sách các vai trò (Roles) trong hệ thống")
                    .createdAt(Instant.now())
                    .build(),

            Permission.builder()
                    .permissionCode("ROLE_ASSIGN_PERMISSION")
                    .permissionName("Gán quyền cho vai trò")
                    .type("ROLE")
                    .description("Cho phép tích chọn và bổ sung thêm các quyền hạn (Permissions) vào một vai trò")
                    .createdAt(Instant.now())
                    .build(),

            Permission.builder()
                    .permissionCode("ROLE_REVOKE_PERMISSION")
                    .permissionName("Tước bỏ quyền của vai trò")
                    .type("ROLE")
                    .description("Cho phép xóa hoặc hạ cấp các quyền hạn đang có ra khỏi một vai trò")
                    .createdAt(Instant.now())
                    .build(),

            Permission.builder()
                    .permissionCode("PERMISSION_VIEW")
                    .permissionName("Xem danh sách tất cả các quyền")
                    .type("PERMISSION")
                    .description("Cho phép xem toàn bộ danh sách các quyền chức năng tĩnh đang có trên hệ thống")
                    .createdAt(Instant.now())
                    .build()
    );

    // Lưu toàn bộ vào MongoDB
    permissionRepository.saveAll(permissions);
    System.out.println(">> Khởi tạo danh sách Quyền (Permissions) thành công!");
}
}