package mealyummy.mealservice.core.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.service.subscription.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpirationScheduler {

    private final SubscriptionService subscriptionService;

    // Chạy vào 00:00:00 mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    public void expireSubscriptions() {
        log.info("Bắt đầu tiến trình kiểm tra và vô hiệu hóa các subscription hết hạn.");
        subscriptionService.processExpiredSubscriptions();
        log.info("Hoàn thành tiến trình kiểm tra subscription.");
    }
}
