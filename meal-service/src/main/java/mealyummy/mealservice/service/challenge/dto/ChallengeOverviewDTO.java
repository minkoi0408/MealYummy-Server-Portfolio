package mealyummy.mealservice.service.challenge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeOverviewDTO {
    private int currentStreak;
    private int highestStreak;
    private String todayDate;
    private boolean breakfastDone;
    private boolean lunchDone;
    private boolean dinnerDone;
    private boolean snackDone;
    private boolean isTodayCompleted;
}
