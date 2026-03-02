package mealyummy.mealservice.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mealyummy.mealservice.service.meal.dto.PriceDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Price {
    private Float minPrice;
    private Float maxPrice;

    public PriceDTO convert(){
        return PriceDTO.builder()
                .minPrice(this.minPrice)
                .maxPrice(this.maxPrice)
                .build();
    }
}
