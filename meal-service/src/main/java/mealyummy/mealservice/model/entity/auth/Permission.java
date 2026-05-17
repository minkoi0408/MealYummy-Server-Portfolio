package mealyummy.mealservice.model.entity.auth;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "permissions")
public class Permission {
    @Id
    private String id;

    @Indexed(unique = true)
    @Field("permission_code")
    private String permissionCode;

    @Field("permission_name")
    private String permissionName;

    @Field("module")
    private String type;

    @Field("description")
    private String description;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
