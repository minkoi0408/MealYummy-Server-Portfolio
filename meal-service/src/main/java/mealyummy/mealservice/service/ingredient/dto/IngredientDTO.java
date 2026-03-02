package mealyummy.mealservice.service.ingredient.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IngredientDTO {
    @Schema(description = "ID tự động sinh của nguyên liệu", accessMode = Schema.AccessMode.READ_ONLY)
    private String id;

    @Schema(description = "Tên nguyên liệu", example = "")
    private String name;

    @Schema(description = "Thời gian tạo nguyên liệu", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdAt;

    @Schema(description = "Trạng thái hoạt động", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean active;
}