package mealyummy.mealservice.core.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.entity.auth.Permission;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.repository.PermissionRepository;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.model.repository.MealRepository;
import mealyummy.mealservice.model.entity.food.Meal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import java.util.Map;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MealRepository mealRepository;



    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        initRoles();
        initPermission();
    }

    private void fixMissingData() {
        log.info("--- FIXING MISSING IMAGES AND NUTRITION ---");
        List<Meal> meals = mealRepository.findAll();
        boolean changedAny = false;
        java.util.Random rand = new java.util.Random();
        
        String[] defaultImages = {
            "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=800&q=80",
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=800&q=80",
            "https://images.unsplash.com/photo-1543339308-43e59d6b73a6?w=800&q=80",
            "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?w=800&q=80",
            "https://images.unsplash.com/photo-1504630083234-14187a9df0f5?w=800&q=80",
            "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=800&q=80",
            "https://images.unsplash.com/photo-1588123190131-1c3fac394f4b?w=800&q=80"
        };

        for (Meal meal : meals) {
            boolean changed = false;
            
            if (meal.getImages() == null || meal.getImages().isEmpty() || meal.getImages().get(0).getUrl() == null || meal.getImages().get(0).getUrl().isEmpty()) {
                List<mealyummy.mealservice.model.pojo.MealImage> images = new java.util.ArrayList<>();
                images.add(mealyummy.mealservice.model.pojo.MealImage.builder()
                        .url(defaultImages[rand.nextInt(defaultImages.length)])
                        .isThumbnail(true)
                        .build());
                meal.setImages(images);
                changed = true;
                log.info("Fixed image for meal: {}", meal.getName());
            }
            
            if (meal.getNutrition() == null || meal.getNutrition().getCalories() == null) {
                mealyummy.mealservice.model.pojo.Nutrition nutrition = mealyummy.mealservice.model.pojo.Nutrition.builder()
                        .calories(300.0 + rand.nextInt(350))
                        .protein(20.0 + rand.nextInt(40))
                        .carbs(30.0 + rand.nextInt(50))
                        .fat(10.0 + rand.nextInt(20))
                        .fiber(5.0 + rand.nextInt(10))
                        .build();
                meal.setNutrition(nutrition);
                changed = true;
                log.info("Fixed nutrition for meal: {}", meal.getName());
            }
            
            if (changed) {
                mealRepository.save(meal);
                changedAny = true;
            }
        }
        if (changedAny) {
            log.info("--- SUCCESSFULLY FIXED MISSING DATA IN DB ---");
        }
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
    if (permissionRepository.count() >= 30) {
        return;
    }
    
    // Xóa data cũ để force tạo lại list mới đầy đủ
    permissionRepository.deleteAll();

    List<Permission> permissions = List.of(
            // Role & Permission
            Permission.builder().permissionCode("ROLE_VIEW").permissionName("Xem danh sách vai trò").type("ROLE").description("Cho phép xem thông tin cấu hình và danh sách các vai trò (Roles) trong hệ thống").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("ROLE_ASSIGN_PERMISSION").permissionName("Gán quyền cho vai trò").type("ROLE").description("Cho phép tích chọn và bổ sung thêm các quyền hạn (Permissions) vào một vai trò").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("ROLE_REVOKE_PERMISSION").permissionName("Tước bỏ quyền của vai trò").type("ROLE").description("Cho phép xóa hoặc hạ cấp các quyền hạn đang có ra khỏi một vai trò").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("PERMISSION_VIEW").permissionName("Xem danh sách tất cả các quyền").type("PERMISSION").description("Cho phép xem toàn bộ danh sách các quyền chức năng tĩnh đang có trên hệ thống").createdAt(Instant.now()).build(),

            // Category
            Permission.builder().permissionCode("VIEW_ALL_CATEGORY").permissionName("Xem danh sách danh mục").type("CATEGORY").description("Cho phép xem danh sách các danh mục món ăn").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("CREATE_CATEGORY").permissionName("Tạo danh mục mới").type("CATEGORY").description("Cho phép tạo mới một danh mục món ăn").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_CATEGORY").permissionName("Cập nhật danh mục").type("CATEGORY").description("Cho phép chỉnh sửa thông tin danh mục").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_STATE_CATEGORY").permissionName("Thay đổi trạng thái danh mục").type("CATEGORY").description("Cho phép ẩn/hiện danh mục").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("DELETE_CATEGORY").permissionName("Xóa danh mục").type("CATEGORY").description("Cho phép xóa danh mục").createdAt(Instant.now()).build(),

            // Tag
            Permission.builder().permissionCode("VIEW_ALL_TAG").permissionName("Xem danh sách thẻ").type("TAG").description("Cho phép xem danh sách các thẻ món ăn").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("CREATE_TAG").permissionName("Tạo thẻ mới").type("TAG").description("Cho phép tạo thẻ món ăn mới").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_TAG").permissionName("Cập nhật thẻ").type("TAG").description("Cho phép chỉnh sửa thông tin thẻ").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_STATE_TAG").permissionName("Thay đổi trạng thái thẻ").type("TAG").description("Cho phép ẩn/hiện thẻ").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("DELETE_TAG").permissionName("Xóa thẻ").type("TAG").description("Cho phép xóa thẻ").createdAt(Instant.now()).build(),

            // Ingredient
            Permission.builder().permissionCode("VIEW_ALL_INGREDIENT").permissionName("Xem danh sách nguyên liệu").type("INGREDIENT").description("Cho phép xem danh sách nguyên liệu").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("CREATE_INGREDIENT").permissionName("Tạo nguyên liệu mới").type("INGREDIENT").description("Cho phép tạo nguyên liệu mới").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_INGREDIENT").permissionName("Cập nhật nguyên liệu").type("INGREDIENT").description("Cho phép chỉnh sửa nguyên liệu").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_STATE_INGREDIENT").permissionName("Thay đổi trạng thái nguyên liệu").type("INGREDIENT").description("Cho phép ẩn/hiện nguyên liệu").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("DELETE_INGREDIENT").permissionName("Xóa nguyên liệu").type("INGREDIENT").description("Cho phép xóa nguyên liệu").createdAt(Instant.now()).build(),

            // Meal
            Permission.builder().permissionCode("VIEW_ALL_MEAL").permissionName("Xem danh sách món ăn").type("MEAL").description("Cho phép xem danh sách món ăn").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("CREATE_MEAL").permissionName("Tạo món ăn mới").type("MEAL").description("Cho phép tạo món ăn mới").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_MEAL").permissionName("Cập nhật món ăn").type("MEAL").description("Cho phép chỉnh sửa món ăn").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_STATE_MEAL").permissionName("Thay đổi trạng thái món ăn").type("MEAL").description("Cho phép ẩn/hiện món ăn").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("DELETE_MEAL").permissionName("Xóa món ăn").type("MEAL").description("Cho phép xóa món ăn").createdAt(Instant.now()).build(),

            // Bundle & Subscription
            Permission.builder().permissionCode("VIEW_ALL_BUNDLE").permissionName("Xem danh sách gói dịch vụ").type("BUNDLE").description("Cho phép xem danh sách gói dịch vụ").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("CREATE_BUNDLE").permissionName("Tạo gói dịch vụ mới").type("BUNDLE").description("Cho phép tạo gói dịch vụ mới").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_BUNDLE").permissionName("Cập nhật gói dịch vụ").type("BUNDLE").description("Cho phép chỉnh sửa gói dịch vụ").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_STATE_BUNDLE").permissionName("Thay đổi trạng thái gói dịch vụ").type("BUNDLE").description("Cho phép ẩn/hiện gói dịch vụ").createdAt(Instant.now()).build(),
            
            Permission.builder().permissionCode("VIEW_ALL_PAYMENT_HISTORY").permissionName("Xem danh sách lịch sử thanh toán").type("PAYMENT").description("Cho phép xem toàn bộ danh sách lịch sử giao dịch thanh toán").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("VIEW_PAYMENT_HISTORY").permissionName("Xem chi tiết thanh toán").type("PAYMENT").description("Cho phép xem chi tiết một lịch sử giao dịch thanh toán").createdAt(Instant.now()).build(),
            
            Permission.builder().permissionCode("VIEW_ALL_USER_SUBSCRIPTION").permissionName("Xem danh sách gói đăng ký của user").type("SUBSCRIPTION").description("Cho phép xem thông tin tất cả gói đăng ký của người dùng").createdAt(Instant.now()).build(),

            // User
            Permission.builder().permissionCode("VIEW_ALL_USER").permissionName("Xem danh sách người dùng").type("USER").description("Cho phép xem danh sách tất cả người dùng").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("VIEW_USER_DETAIL").permissionName("Xem chi tiết người dùng").type("USER").description("Cho phép xem chi tiết thông tin một người dùng").createdAt(Instant.now()).build(),
            Permission.builder().permissionCode("UPDATE_USER_ROLE").permissionName("Cập nhật vai trò người dùng").type("USER").description("Cho phép thay đổi vai trò (Role) của người dùng").createdAt(Instant.now()).build()
    );

    permissionRepository.saveAll(permissions);
    System.out.println(">> Khởi tạo danh sách Quyền (Permissions) thành công!");
}
}