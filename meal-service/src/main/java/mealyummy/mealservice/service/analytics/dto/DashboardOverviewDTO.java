package mealyummy.mealservice.service.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewDTO {
    private long totalUsers;
    private double totalRevenue;
    private long activeSubscriptions;
    private long totalMeals;
    
    private String userChangeStr;
    private String revenueChangeStr;
    private String subscriptionChangeStr;
    private String mealChangeStr;

    private List<RevenueDataPointDTO> revenueChart;
    private List<RecentTransactionDTO> recentTransactions;
}
