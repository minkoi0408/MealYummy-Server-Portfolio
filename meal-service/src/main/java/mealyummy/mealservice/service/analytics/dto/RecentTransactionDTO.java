package mealyummy.mealservice.service.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionDTO {
    private String transactionId;
    private String username;
    private String email;
    private String avatarUrl;
    private String bundleName;
    private double amount;
    private String date;
}
