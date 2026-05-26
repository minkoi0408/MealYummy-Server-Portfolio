package mealyummy.mealservice.service.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.service.iam.role.dto.RoleResponse;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private String id;
    private String username;
    private String email;
    private String avatarUrl;
    private boolean isActive;
    private boolean isTotpEnabled;
    private Instant createdAt;
    private RoleResponse role;

    public static UserResponseDTO fromEntity(User user) {
        if (user == null) return null;
        
        RoleResponse roleResponse = null;
        if (user.getRole() != null) {
            roleResponse = RoleResponse.builder()
                    .roleName(user.getRole().getRoleName())
                    .description(user.getRole().getDescription())
                    // Assuming permissions might be set here or just leave them null if not fully mapped
                    .build();
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.isActive())
                .isTotpEnabled(user.isTotpEnabled())
                .createdAt(user.getCreatedAt())
                .role(roleResponse)
                .build();
    }
}
