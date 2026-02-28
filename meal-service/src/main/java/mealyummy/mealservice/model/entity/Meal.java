package mealyummy.mealservice.model.entity;

import mealyummy.mealservice.model.pojo.Meal_Image;
import mealyummy.mealservice.model.pojo.Price;
import org.springframework.data.mongodb.core.mapping.Document;

import org.springframework.data.annotation.Id;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.List;

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
    private List<Meal_Image> images;

    @DocumentReference(lazy = true)
    private List<Category> categories;

    @DocumentReference(lazy = true)
    private List<Tag> tags;

    @DocumentReference(lazy = true)
    private List<Ingredient> ingredients;

}