package mealyummy.mealservice.service.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteStatusResponseDTO {
    private boolean isFavorite;
}
