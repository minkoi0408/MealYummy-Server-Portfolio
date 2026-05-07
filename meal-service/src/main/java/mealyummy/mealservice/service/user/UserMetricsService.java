package mealyummy.mealservice.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.User;
import mealyummy.mealservice.model.entity.UserMetrics;
import mealyummy.mealservice.model.repository.UserMetricsRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.user.dto.UserMetricsDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserMetricsService {

    private final UserRepository userRepository;
    private final UserMetricsRepository userMetricsRepository;

    public UserMetricsDTO getUserMetrics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMetricsRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .map(m -> UserMetricsDTO.builder()
                        .weight(m.getWeight())
                        .height(m.getHeight())
                        .age(m.getAge())
                        .gender(m.getGender())
                        .activity(m.getActivity())
                        .goal(m.getGoal())
                        .bodyFat(m.getBodyFat())
                        .muscleMass(m.getMuscleMass())
                        .updatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt())
                        .build())
                .orElse(null);
    }

    public void updateUserMetrics(String username, UserMetricsDTO dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Luôn tạo bản ghi mới để người dùng có thể test biểu đồ lịch sử dễ dàng
        UserMetrics metrics = UserMetrics.builder().userId(user.getId()).build();

        metrics.setWeight(dto.getWeight());
        metrics.setHeight(dto.getHeight());
        metrics.setAge(dto.getAge());
        metrics.setGender(dto.getGender());
        metrics.setActivity(dto.getActivity());
        metrics.setGoal(dto.getGoal());
        metrics.setBodyFat(dto.getBodyFat());
        metrics.setMuscleMass(dto.getMuscleMass());
        
        metrics.setCreatedAt(Instant.now());
        metrics.setUpdatedAt(Instant.now());

        userMetricsRepository.save(metrics);
        log.info("Đã lưu chỉ số sức khỏe mới cho user: {}", username);
    }

    public List<UserMetricsDTO> getUserMetricsHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMetricsRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(m -> UserMetricsDTO.builder()
                        .weight(m.getWeight())
                        .height(m.getHeight())
                        .age(m.getAge())
                        .gender(m.getGender())
                        .activity(m.getActivity())
                        .goal(m.getGoal())
                        .bodyFat(m.getBodyFat())
                        .muscleMass(m.getMuscleMass())
                        .updatedAt(m.getUpdatedAt() != null ? m.getUpdatedAt() : m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
