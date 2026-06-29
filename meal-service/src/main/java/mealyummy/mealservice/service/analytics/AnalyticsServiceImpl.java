package mealyummy.mealservice.service.analytics;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.food.Meal;
import mealyummy.mealservice.model.entity.subscription.Bundle;
import mealyummy.mealservice.model.entity.subscription.PaymentHistory;
import mealyummy.mealservice.model.entity.subscription.UserSubscription;
import mealyummy.mealservice.service.analytics.dto.DashboardOverviewDTO;
import mealyummy.mealservice.service.analytics.dto.RecentTransactionDTO;
import mealyummy.mealservice.service.analytics.dto.RevenueDataPointDTO;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final MongoTemplate mongoTemplate;

    @Override
    public DashboardOverviewDTO getDashboardOverview() {
        long totalUsers = mongoTemplate.count(new Query(), User.class);
        long totalMeals = mongoTemplate.count(new Query(), Meal.class);
        
        Criteria activeCriteria = Criteria.where("status").is("ACTIVE");
        long activeSubscriptions = mongoTemplate.count(new Query(activeCriteria), UserSubscription.class);

        Criteria successPayment = Criteria.where("paymentStatus").is("SUCCESS");
        List<PaymentHistory> allSuccess = mongoTemplate.find(new Query(successPayment), PaymentHistory.class);

        double totalRevenue = 0;
        double thisMonthRevenue = 0;
        int currentMonth = Instant.now().atZone(ZoneId.systemDefault()).getMonthValue();
        int currentYear = Instant.now().atZone(ZoneId.systemDefault()).getYear();
        
        double[] monthlyRevenues = new double[12];
        
        for (PaymentHistory p : allSuccess) {
            if (p.getCreatedAt() != null) {
                var zdt = p.getCreatedAt().atZone(ZoneId.systemDefault());
                if (zdt.getYear() == currentYear) {
                    monthlyRevenues[zdt.getMonthValue() - 1] += p.getAmount();
                }
                if (zdt.getYear() == currentYear && zdt.getMonthValue() == currentMonth) {
                    thisMonthRevenue += p.getAmount();
                }
            }
            totalRevenue += p.getAmount();
        }
        
        List<RevenueDataPointDTO> chartData = new ArrayList<>();
        String[] monthLabels = {"T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"};
        for (int i = 0; i < 12; i++) {
            chartData.add(new RevenueDataPointDTO(monthLabels[i], monthlyRevenues[i]));
        }

        // Recent transactions
        Query recentQuery = new Query(successPayment).with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(5);
        List<PaymentHistory> recentPayments = mongoTemplate.find(recentQuery, PaymentHistory.class);
        
        List<RecentTransactionDTO> recentList = recentPayments.stream().map(p -> {
            User user = mongoTemplate.findById(p.getUserId(), User.class);
            String username = user != null ? user.getUsername() : "Unknown User";
            String email = user != null ? user.getEmail() : "No Email";
            String avatar = user != null ? user.getAvatarUrl() : "";
            
            Bundle bundle = null;
            if (p.getBundleId() != null) {
                bundle = mongoTemplate.findById(p.getBundleId(), Bundle.class);
            }
            String bundleName = bundle != null ? bundle.getName() : "Premium Bundle";
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
            String dateStr = p.getCreatedAt() != null ? formatter.format(p.getCreatedAt()) : "";
            
            return RecentTransactionDTO.builder()
                    .transactionId(p.getTransactionId())
                    .username(username)
                    .email(email)
                    .avatarUrl(avatar)
                    .amount(p.getAmount())
                    .bundleName(bundleName)
                    .date(dateStr)
                    .build();
        }).collect(Collectors.toList());


        return DashboardOverviewDTO.builder()
                .totalUsers(totalUsers)
                .totalMeals(totalMeals)
                .activeSubscriptions(activeSubscriptions)
                .totalRevenue(totalRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .userChangeStr("+12%")
                .revenueChangeStr("+8.4%")
                .subscriptionChangeStr("-2%")
                .mealChangeStr("+5%")
                .recentTransactions(recentList)
                .revenueChart(chartData)
                .build();
    }
}
