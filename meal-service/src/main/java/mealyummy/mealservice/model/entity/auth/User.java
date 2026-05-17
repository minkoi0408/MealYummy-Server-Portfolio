package mealyummy.mealservice.model.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;

    @Indexed(unique = true)
    private String email;

    private String avatarUrl;

    @Builder.Default
    private boolean isActive = true;

    // TOTP (Google Authenticator) fields
    private String totpSecret;
    @Builder.Default
    private boolean isTotpEnabled = false;

    @CreatedDate
    private Instant createdAt;

    @DocumentReference(lazy = true)
    private Role role;
}
