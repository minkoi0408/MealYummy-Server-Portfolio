package mealyummy.mealservice.service.analytics;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.food.Meal;
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

        // Sum revenue
        Criteria successPayment = Criteria.where("paymentStatus").is("SUCCESS");
        Aggregation sumAggregation = Aggregation.newAggregation(
                Aggregation.match(successPayment),
                Aggregation.group().sum("amount").as("totalRevenue")
        );
        AggregationResults<org.bson.Document> sumResults = mongoTemplate.aggregate(sumAggregation, PaymentHistory.class, org.bson.Document.class);
        double totalRevenue = 0;
        if (!sumResults.getMappedResults().isEmpty()) {
            Object revObj = sumResults.getMappedResults().get(0).get("totalRevenue");
            if (revObj instanceof Number) {
                totalRevenue = ((Number) revObj).doubleValue();
            }
        }

        // Recent transactions
        Query recentQuery = new Query(successPayment).with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(5);
        List<PaymentHistory> recentPayments = mongoTemplate.find(recentQuery, PaymentHistory.class);
        
        List<RecentTransactionDTO> recentList = recentPayments.stream().map(p -> {
            User user = mongoTemplate.findById(p.getUserId(), User.class);
            String username = user != null ? user.getUsername() : "Unknown User";
            String email = user != null ? user.getEmail() : "No Email";
            String avatar = user != null ? user.getAvatarUrl() : "";
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
            String dateStr = p.getCreatedAt() != null ? formatter.format(p.getCreatedAt()) : "";
            
            return RecentTransactionDTO.builder()
                    .transactionId(p.getTransactionId())
                    .username(username)
                    .email(email)
                    .avatarUrl(avatar)
                    .amount(p.getAmount())
                    .bundleName("Premium Bundle")
                    .date(dateStr)
                    .build();
        }).collect(Collectors.toList());

        // Dummy chart data for now, could be aggregated properly
        List<RevenueDataPointDTO> chartData = new ArrayList<>();
        chartData.add(new RevenueDataPointDTO("T1", totalRevenue * 0.1));
        chartData.add(new RevenueDataPointDTO("T2", totalRevenue * 0.15));
        chartData.add(new RevenueDataPointDTO("T3", totalRevenue * 0.2));
        chartData.add(new RevenueDataPointDTO("T4", totalRevenue * 0.25));
        chartData.add(new RevenueDataPointDTO("T5", totalRevenue * 0.1));
        chartData.add(new RevenueDataPointDTO("T6", totalRevenue * 0.2));

        return DashboardOverviewDTO.builder()
                .totalUsers(totalUsers)
                .totalMeals(totalMeals)
                .activeSubscriptions(activeSubscriptions)
                .totalRevenue(totalRevenue)
                .userChangeStr("+12%")
                .revenueChangeStr("+8.4%")
                .subscriptionChangeStr("-2%")
                .mealChangeStr("+5%")
                .recentTransactions(recentList)
                .revenueChart(chartData)
                .build();
    }
}
