package mealyummy.mealservice.service.iam.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String username;
    private String email;
    private String avatarUrl;
    private String accessToken;
    private String refreshToken;
    private java.time.Instant createdAt;
}
