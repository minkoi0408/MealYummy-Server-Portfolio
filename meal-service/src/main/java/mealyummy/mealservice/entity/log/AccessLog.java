package mealyummy.mealservice.entity.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "access_logs")
public class AccessLog {

    @Id
    private String id;

    private String userId;
    
    private String username;

    private String ipAddress;

    private String endpoint;

    private String method;

    private String userAgent;

    private int status; // HTTP Status Code

    private long durationMs; // Time taken to process the request

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}