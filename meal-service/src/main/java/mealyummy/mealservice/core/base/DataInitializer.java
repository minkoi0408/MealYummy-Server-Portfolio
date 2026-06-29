package mealyummy.mealservice.core.base;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.entity.auth.Permission;
import mealyummy.mealservice.model.entity.auth.Role;
import mealyummy.mealservice.model.repository.PermissionRepository;
import mealyummy.mealservice.model.repository.RoleRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.repository.MealRepository;
import mealyummy.mealservice.model.entity.food.Meal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import mealyummy.mealservice.service.iam.role.PermissionSyncService;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MealRepository mealRepository;
    private final PermissionSyncService permissionSyncService;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        Set<String> allApiPermissions = initPermission();
        
        // Chỉ khởi tạo Roles và gán Role cho User NẾU DB chưa có Role nào
        if (roleRepository.count() == 0) {
            initRoles(allApiPermissions);
        } else {
            log.info("--- Roles đã tồn tại, bỏ qua bước khởi tạo Roles ---");
        }
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

    private void initRoles(Set<String> allApiPermissions) {
        log.info("--- Khởi tạo lại Roles và đồng bộ Permissions ---");

        Role adminRole = Role.builder()
                .roleCode("ROLE_ADMIN")
                .roleName("Admin")
                .description("Quản trị viên tối cao của hệ thống")
                .permissions(new HashSet<>(allApiPermissions)) // Admin có tất cả quyền
                .build();

        // Membership có thể được cấu hình thêm sau. Tạm thời cho GET.
        Set<String> getPermissions = new HashSet<>();
        for (String p : allApiPermissions) {
            if (p.startsWith("GET_")) getPermissions.add(p);
        }

        Role membershipRole = Role.builder()
                .roleCode("ROLE_MEMBERSHIP")
                .roleName("Membership")
                .description("Tài khoản người dùng đã đăng ký gói trả phí")
                .permissions(getPermissions)
                .build();

        Role freeRole = Role.builder()
                .roleCode("ROLE_FREE")
                .roleName("Free")
                .description("Tài khoản người dùng miễn phí mặc định")
                .permissions(getPermissions) // Free cũng cho phép GET (có thể tuỳ chỉnh sau)
                .build();

        roleRepository.saveAll(List.of(freeRole, membershipRole, adminRole));
        log.info("--- Đã khởi tạo thành công 3 Roles: FREE, MEMBERSHIP, ADMIN ---");

        // FIX BROKEN USER REFERENCES
        log.info("--- Cập nhật lại các liên kết Role bị hỏng cho User ---");
        List<User> users = userRepository.findAll();
        boolean usersUpdated = false;
        for (User u : users) {
            if ("nhuthuy.work@gmail.com".equalsIgnoreCase(u.getEmail()) || "Nhựt Huy Nguyễn".equalsIgnoreCase(u.getUsername())) {
                u.setRole(adminRole);
                usersUpdated = true;
            } else {
                u.setRole(freeRole);
                usersUpdated = true;
            }
        }
        if (usersUpdated) {
            userRepository.saveAll(users);
            log.info("--- Đã khắc phục thành công lỗi mất Role của User ---");
        }
    }

    private Set<String> initPermission() {
        
        // Quét API
        Set<String> scannedPermissions = permissionSyncService.syncApiPermissions();
        log.info(">> Khởi tạo danh sách Quyền từ API Scanner thành công! Tổng số: " + scannedPermissions.size());
        
        return scannedPermissions;
    }
}
