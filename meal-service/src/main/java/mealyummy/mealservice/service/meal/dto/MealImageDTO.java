package mealyummy.mealservice.service.meal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MealImageDTO {
    @Schema(description = "Đường dẫn hình ảnh (URL Cloudinary/S3...)", example = "https://res.cloudinary.com/.../bun-bo.jpg")
    private String url;

    @Schema(description = "Hình ảnh này có phải là ảnh bìa chính không?", example = "true")
    private Boolean isThumbnail;
}
