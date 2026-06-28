package mealyummy.mealservice.model.entity.food;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "favorite_meals")
public class FavoriteMeal {
    @Id
    private String id;
    private String userId;
    private String mealId;

    @CreatedDate
    private Instant createdAt;
}
