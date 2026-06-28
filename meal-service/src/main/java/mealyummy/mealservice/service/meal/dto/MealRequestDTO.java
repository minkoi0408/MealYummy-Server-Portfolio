package mealyummy.mealservice.service.meal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload yêu cầu tạo mới món ăn")
public class MealRequestDTO {

    @NotBlank(message = "Tên món ăn không được để trống")
    @Schema(description = "Tên món ăn", example = "Bún bò Huế")
    private String name;

    @Schema(description = "Mô tả chi tiết món ăn", example = "Bún bò cay nồng chuẩn vị, nước dùng đậm đà hầm từ xương bò 12 tiếng.")
    private String description;


    @Schema(description = "Danh sách hình ảnh món ăn")
    private List<MealImageDTO> images;

    @NotEmpty(message = "Món ăn phải thuộc ít nhất 1 danh mục")
    @Schema(description = "Danh sách các danh mục", example = "[\"Ăn sáng\", \"Keto\"]")
    private List<String> categories;

    @Schema(description = "Danh sách các thẻ (tag)", example = "[\"Vegan\", \"Gluten Free\"]")
    private List<String> tags;

    @NotEmpty(message = "Món ăn phải có ít nhất 1 nguyên liệu")
    @Valid // Kích hoạt kiểm tra validation cho từng phần tử trong list
    @Schema(description = "Danh sách nguyên liệu và định lượng")
    private List<MealIngredientDTO> ingredients;

    @Valid
    @Schema(description = "Thông tin dinh dưỡng")
    private NutritionDTO nutrition;
}