package mealyummy.mealservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MealServiceApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner checkRedis(org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        return args -> {
            try {
                redisTemplate.opsForValue().set("health-check", "ok");
                String val = redisTemplate.opsForValue().get("health-check");
                System.out.println("🚀 [REDIS] Kết nối THÀNH CÔNG! Kiểm tra: " + val);
            } catch (Exception e) {
                System.err.println("❌ [REDIS] Kết nối THẤT BẠI! Hãy kiểm tra Docker hoặc cổng 6379. Lỗi: " + e.getMessage());
            }
        };
    }
}