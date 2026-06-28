package mealyummy.mealservice.service.meal.dto;

import java.util.List;

import lombok.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MealResponseDTO {

    // --- VIEW PUBLIC: Dành cho dạng danh sách (List/Card) ---

    private String id;

    private String name;


    private List<MealImageDTO> images;

    // --- VIEW DETAIL: Dành cho trang xem chi tiết (Detail) ---

    private String description;

    private List<String> categories;

    private List<String> tags;

    private List<MealIngredientDTO> ingredients;

    private NutritionDTO nutrition;

    // --- VIEW INTERNAL: Dành cho Admin/Hệ thống ---

    private String createdAt; // Format: T3, 10:30:50 20-02-2026

    private Boolean active;
}