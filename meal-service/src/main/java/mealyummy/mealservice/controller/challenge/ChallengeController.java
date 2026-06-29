package mealyummy.mealservice.controller.challenge;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.challenge.DailyMealRecord;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.challenge.ChallengeService;
import mealyummy.mealservice.service.challenge.dto.ChallengeOverviewDTO;
import mealyummy.mealservice.service.challenge.dto.CheckMealRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;
    private final UserRepository userRepository;

    private String getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return currentUser.getId();
    }

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/today")
    public ResponseEntity<BaseApiResponse<ChallengeOverviewDTO>> getTodayOverview() {
        String userId = getCurrentUserId();
        ChallengeOverviewDTO response = challengeService.getTodayOverview(userId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lay thong tin thu thach hom nay", response));
    }

    @PreAuthorize("@apiAuth.check()")
    @PostMapping("/check")
    public ResponseEntity<BaseApiResponse<ChallengeOverviewDTO>> checkMeal(@Valid @RequestBody CheckMealRequest request) {
        String userId = getCurrentUserId();
        ChallengeOverviewDTO response = challengeService.checkMeal(userId, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Cap nhat thu thach thanh cong", response));
    }

    @PreAuthorize("@apiAuth.check()")
    @GetMapping("/history")
    public ResponseEntity<BaseApiResponse<List<DailyMealRecord>>> getHistory(@RequestParam String yearMonth) {
        String userId = getCurrentUserId();
        List<DailyMealRecord> response = challengeService.getHistoryByMonth(userId, yearMonth);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Lay lich su thu thach thang " + yearMonth, response));
    }
}
