package mealyummy.mealservice.core.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
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
}