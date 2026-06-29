package mealyummy.mealservice.service.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDataPointDTO {
    private String label;
    private double revenue;
}
