package mealyummy.mealservice.service.challenge;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.challenge.DailyMealRecord;
import mealyummy.mealservice.model.entity.challenge.UserStreak;
import mealyummy.mealservice.model.repository.challenge.DailyMealRecordRepository;
import mealyummy.mealservice.model.repository.challenge.UserStreakRepository;
import mealyummy.mealservice.service.challenge.dto.ChallengeOverviewDTO;
import mealyummy.mealservice.service.challenge.dto.CheckMealRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final DailyMealRecordRepository dailyMealRecordRepository;
    private final UserStreakRepository userStreakRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public ChallengeOverviewDTO getTodayOverview(String userId) {
        String todayStr = LocalDate.now().format(formatter);
        DailyMealRecord record = dailyMealRecordRepository.findByUserIdAndRecordDate(userId, todayStr)
                .orElse(DailyMealRecord.builder()
                        .userId(userId)
                        .recordDate(todayStr)
                        .breakfastDone(false)
                        .lunchDone(false)
                        .dinnerDone(false)
                        .snackDone(false)
                        .isCompleted(false)
                        .build());

        UserStreak streak = getOrCreateStreak(userId);

        // Check if streak is broken (missed yesterday and today is not completed yet)
        checkAndResetBrokenStreak(streak, todayStr);

        return buildOverview(streak, record);
    }

    @Override
    @Transactional
    public ChallengeOverviewDTO checkMeal(String userId, CheckMealRequest request) {
        String todayStr = LocalDate.now().format(formatter);
        DailyMealRecord record = dailyMealRecordRepository.findByUserIdAndRecordDate(userId, todayStr)
                .orElse(DailyMealRecord.builder()
                        .userId(userId)
                        .recordDate(todayStr)
                        .build());

        // Save original completion status
        boolean wasCompleted = record.isCompleted();

        // Update meal status
        switch (request.getMealType().toUpperCase()) {
            case "BREAKFAST":
                record.setBreakfastDone(request.getStatus());
                break;
            case "LUNCH":
                record.setLunchDone(request.getStatus());
                break;
            case "DINNER":
                record.setDinnerDone(request.getStatus());
                break;
            case "SNACK":
                record.setSnackDone(request.getStatus());
                break;
        }

        // Evaluate day completion (requiring 3 main meals)
        boolean isNowCompleted = record.isBreakfastDone() && record.isLunchDone() && record.isDinnerDone();
        record.setCompleted(isNowCompleted);

        dailyMealRecordRepository.save(record);

        UserStreak streak = getOrCreateStreak(userId);
        checkAndResetBrokenStreak(streak, todayStr);

        // If newly completed today
        if (!wasCompleted && isNowCompleted) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            if (streak.getCurrentStreak() > streak.getHighestStreak()) {
                streak.setHighestStreak(streak.getCurrentStreak());
            }
            streak.setLastActiveDate(todayStr);
            userStreakRepository.save(streak);
        }
        // If un-completed today (user unticked)
        else if (wasCompleted && !isNowCompleted) {
            streak.setCurrentStreak(Math.max(0, streak.getCurrentStreak() - 1));
            // Assuming they un-ticked today, lastActiveDate should revert to yesterday.
            // For simplicity, we just set it to yesterday
            LocalDate yesterday = LocalDate.now().minusDays(1);
            streak.setLastActiveDate(yesterday.format(formatter));
            userStreakRepository.save(streak);
        }

        return buildOverview(streak, record);
    }

    @Override
    public List<DailyMealRecord> getHistoryByMonth(String userId, String yearMonth) {
        return dailyMealRecordRepository.findByUserIdAndRecordDateStartingWith(userId, yearMonth);
    }

    private UserStreak getOrCreateStreak(String userId) {
        return userStreakRepository.findByUserId(userId)
                .orElse(UserStreak.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .highestStreak(0)
                        .build());
    }

    private void checkAndResetBrokenStreak(UserStreak streak, String todayStr) {
        if (streak.getLastActiveDate() == null || streak.getCurrentStreak() == 0) return;

        LocalDate today = LocalDate.parse(todayStr, formatter);
        LocalDate lastActive = LocalDate.parse(streak.getLastActiveDate(), formatter);
        
        long daysBetween = ChronoUnit.DAYS.between(lastActive, today);

        // If more than 1 day has passed since last active, streak is broken
        if (daysBetween > 1) {
            streak.setCurrentStreak(0);
            userStreakRepository.save(streak);
        }
    }

    private ChallengeOverviewDTO buildOverview(UserStreak streak, DailyMealRecord record) {
        return ChallengeOverviewDTO.builder()
                .currentStreak(streak.getCurrentStreak())
                .highestStreak(streak.getHighestStreak())
                .todayDate(record.getRecordDate())
                .breakfastDone(record.isBreakfastDone())
                .lunchDone(record.isLunchDone())
                .dinnerDone(record.isDinnerDone())
                .snackDone(record.isSnackDone())
                .isTodayCompleted(record.isCompleted())
                .build();
    }
}
