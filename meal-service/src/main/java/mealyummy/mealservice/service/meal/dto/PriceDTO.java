package mealyummy.mealservice.service.meal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceDTO {

    @NotNull(message = "Giá tối thiểu không được để trống")
    @Min(value = 0, message = "Giá tối thiểu không được âm")
    @Schema(description = "Giá thấp nhất của món ăn (VD: Size nhỏ/thường)", example = "45000")
    private Float minPrice;

    @NotNull(message = "Giá tối đa không được để trống")
    @Min(value = 0, message = "Giá tối đa không được âm")
    @Schema(description = "Giá cao nhất của món ăn (VD: Size lớn/đặc biệt). Nếu món chỉ có 1 giá, điền giống minPrice.", example = "60000")
    private Float maxPrice;
}