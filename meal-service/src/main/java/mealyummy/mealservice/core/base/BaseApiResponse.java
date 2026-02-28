package mealyummy.mealservice.core.base;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseApiResponse<T> {
    private String timestamp;
    private String message;
    private T data;
    private Map<String, String> errors;

    public static <T> BaseApiResponse<T> ok(String message, T data) {
        return new BaseApiResponse<>
                (Instant.now().toString(), message, data, null);
    }
    public static <T> BaseApiResponse<T> error(String message, Map<String, String> errors) {
        return new BaseApiResponse<>
                (Instant.now().toString(), message, null, errors);
    }
}