package mealyummy.mealservice.service.iam.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.model.entity.auth.Permission;
import mealyummy.mealservice.model.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionSyncService {

    private final RequestMappingHandlerMapping handlerMapping;
    private final PermissionRepository permissionRepository;

    /**
     * Scan tat ca cac API (/api/**) va cap nhat vao Collection permissions.
     * Tra ve danh sach tat ca cac PermissionCode da duoc scan.
     */
    public Set<String> syncApiPermissions() {
        log.info("B?t d?u scan d?ng b? API Permissions...");
        Set<String> scannedPermissionCodes = new HashSet<>();

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            
            Set<String> patterns = new HashSet<>();
            if (mappingInfo.getPathPatternsCondition() != null) {
                patterns.addAll(mappingInfo.getPathPatternsCondition().getPatternValues());
            } else if (mappingInfo.getPatternsCondition() != null) {
                patterns.addAll(mappingInfo.getPatternsCondition().getPatterns());
            }

            for (String pattern : patterns) {
                if (pattern.startsWith("/api/")) {
                    Set<org.springframework.web.bind.annotation.RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();
                    if (methods.isEmpty()) {
                        continue;
                    }

                    for (org.springframework.web.bind.annotation.RequestMethod method : methods) {
                        String methodStr = method.name();
                        String permissionCode = methodStr + "_" + pattern;
                        scannedPermissionCodes.add(permissionCode);

                        // Kiem tra trong DB neu chua co thi tao moi
                        if (!permissionRepository.existsByPermissionCode(permissionCode)) {
                            // Trich xuat module tu duong dan (e.g. /api/v1/meals -> meals)
                            String[] pathParts = pattern.split("/");
                            String module = pathParts.length > 3 ? pathParts[3].toUpperCase() : "GENERAL";
                            
                            Permission newPerm = Permission.builder()
                                    .permissionCode(permissionCode)
                                    .permissionName("API: " + methodStr + " " + pattern)
                                    .type(module)
                                    .description("Tu d?ng tao tu API Scanner")
                                    .createdAt(Instant.now())
                                    .build();
                            permissionRepository.save(newPerm);
                        }
                    }
                }
            }
        }
        log.info("D?ng b? ho?n t?t {} API permissions.", scannedPermissionCodes.size());
        return scannedPermissionCodes;
    }
}
