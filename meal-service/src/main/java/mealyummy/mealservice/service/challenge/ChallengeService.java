package mealyummy.mealservice.service.challenge;

import mealyummy.mealservice.model.entity.challenge.DailyMealRecord;
import mealyummy.mealservice.service.challenge.dto.ChallengeOverviewDTO;
import mealyummy.mealservice.service.challenge.dto.CheckMealRequest;

import java.util.List;

public interface ChallengeService {
    ChallengeOverviewDTO getTodayOverview(String userId);
    ChallengeOverviewDTO checkMeal(String userId, CheckMealRequest request);
    List<DailyMealRecord> getHistoryByMonth(String userId, String yearMonth);
}
