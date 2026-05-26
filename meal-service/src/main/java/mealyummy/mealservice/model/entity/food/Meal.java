package mealyummy.mealservice.model.entity.food;

import mealyummy.mealservice.model.pojo.MealImage;
import mealyummy.mealservice.model.pojo.MealIngredient;
import mealyummy.mealservice.model.pojo.Nutrition;
import mealyummy.mealservice.model.pojo.Price;
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
@Document(collection = "meals")
public class Meal {

    @Id
    private String id;
    private String name;
    private String description;
    private Price price;
    private List<MealImage> images;
    private Nutrition nutrition;

    @DocumentReference(lazy = true)
    private List<Category> categories;

    @DocumentReference(lazy = true)
    private List<Tag> tags;

    private List<MealIngredient> ingredients;

    @CreatedDate
    private Instant createdAt;

    @Builder.Default
    private Boolean active = true;
}