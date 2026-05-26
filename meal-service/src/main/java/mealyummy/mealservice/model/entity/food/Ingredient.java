package mealyummy.mealservice.model.entity.food;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.core.util.DateTimeFormat;
import mealyummy.mealservice.service.ingredient.dto.IngredientDTO;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ingredients")
public class Ingredient {
    @Id
    private String id;
    private String name;
    private String description;
    @CreatedDate
    private Instant createdAt;

    @Builder.Default
    private Boolean active = true;

    public IngredientDTO convert() {
        return IngredientDTO.builder()
                .id(this.getId())
                .name(this.getName())
                .description(this.getDescription())
                .createdAt(DateTimeFormat.formatInstantCustom(this.getCreatedAt()))
                .active(this.getActive())
                .build();
    }
}
