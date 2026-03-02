package mealyummy.mealservice.service.meal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import mealyummy.mealservice.model.enums.IngredientUnit;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MealIngredientDTO {
    @NotBlank(message = "ID nguyên liệu không được để trống")
    @Schema(description = "ID của nguyên liệu lấy từ bảng Ingredients", example = "65f1a2b3c4d5e6f7a8b9c123")
    private String ingredientId;

    @Schema(description = "Tên nguyên liệu", accessMode = Schema.AccessMode.READ_ONLY)
    private String name;

    @NotNull(message = "Định lượng không được để trống")
    @Min(value = 0, message = "Định lượng phải lớn hơn 0")
    @Schema(description = "Số lượng nguyên liệu", example = "200")
    private Double value;

    @NotNull(message = "Đơn vị không được để trống")
    @Schema(description = "Đơn vị đo lường (G, KG, ML, L, TSP, TBSP, PIECE, CLOVE, BUNCH, BOWL, PINCH)", example = "G")
    private IngredientUnit unit;

    @Schema(description = "Tên đơn vị", accessMode = Schema.AccessMode.READ_ONLY)
    private String unitDesc;
}
