package mealyummy.mealservice.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    @Value("${app.client-url:http://localhost:5173}")
    private String clientUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Chỉ áp dụng cho các đường dẫn bắt đầu bằng /api
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173", clientUrl) // Cho phép Frontend Vite
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Các phương thức được phép
                        .allowedHeaders("*") // Cho phép tất cả các Header
                        .allowCredentials(true); // Quan trọng: Cho phép gửi kèm Cookie/Auth Header
            }
        };
    }
}
