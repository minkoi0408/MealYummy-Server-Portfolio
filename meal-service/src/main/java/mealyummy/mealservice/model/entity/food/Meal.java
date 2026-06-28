package mealyummy.mealservice.model.entity.food;

import mealyummy.mealservice.model.pojo.MealImage;
import mealyummy.mealservice.model.pojo.MealIngredient;
import mealyummy.mealservice.model.pojo.Nutrition;
import mealyummy.mealservice.model.pojo.Nutrition;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.List;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
/**
 * @author Nonoru
 * @version v1
 * @since 28-02-2026
 * */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meals_v2")
public class Meal {

    @Id
    private String id;
    private String name;
    private String description;

    private List<MealImage> images;
    private Nutrition nutrition;

    private List<String> categories;

    private List<String> tags;

    private List<MealIngredient> ingredients;

    @CreatedDate
    private Instant createdAt;

    private List<Double> embedding;

    @Builder.Default
    private Boolean active = true;
}