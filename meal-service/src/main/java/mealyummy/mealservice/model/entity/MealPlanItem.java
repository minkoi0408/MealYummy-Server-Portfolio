package mealyummy.mealservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "meal_plan")
public class MealPlanItem {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String recipeId;
    private String recipeName;
    private String recipeImage;
    private String calories;
    private String cookTime;

    private String date; // "YYYY-MM-DD"
    private String mealType; // "breakfast", "lunch", "dinner", "snack"

    private boolean isEaten;

    @CreatedDate
    private Instant createdAt;
}
