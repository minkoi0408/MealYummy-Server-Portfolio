package mealyummy.mealservice.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleDuration {
    
    @Field("duration_code")
    private String durationCode; // e.g. "1_MONTH", "3_MONTHS", "1_YEAR"
    
    @Field("duration_in_days")
    private int durationInDays; // e.g. 30, 90, 365
    
    @Field("price")
    private double price;
}
