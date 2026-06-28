package mealyummy.mealservice.model.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "roles")
public class Role {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("role_code")
    private String roleCode;

    @Field("role_name")
    private String roleName;

    @Field("description")
    private String description;

    @Field("is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    private Instant createdAt;

    @Field("permissions")
    private Set<String> permissions;
}
