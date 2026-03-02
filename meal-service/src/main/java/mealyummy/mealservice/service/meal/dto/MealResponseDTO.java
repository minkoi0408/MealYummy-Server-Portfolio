package mealyummy.mealservice.service.meal.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import mealyummy.mealservice.core.view.View;
import mealyummy.mealservice.service.category.dto.CategoryDTO;
import mealyummy.mealservice.service.tag.dto.TagDTO;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MealResponseDTO {

    // --- VIEW PUBLIC: Dành cho dạng danh sách (List/Card) ---

    private String id;

    private String name;

    private PriceDTO price;

    private List<MealImageDTO> images;

    // --- VIEW DETAIL: Dành cho trang xem chi tiết (Detail) ---

    @JsonView(View.Detail.class)
    private String description;

    @JsonView(View.Detail.class)
    private List<CategoryDTO> categories;

    @JsonView(View.Detail.class)
    private List<TagDTO> tags;

    @JsonView(View.Detail.class)
    private List<MealIngredientDTO> ingredients;

    // --- VIEW INTERNAL: Dành cho Admin/Hệ thống ---

    @JsonView(View.Internal.class)
    private String createdAt; // Format: T3, 10:30:50 20-02-2026

    @JsonView(View.Internal.class)
    private Boolean active;
}