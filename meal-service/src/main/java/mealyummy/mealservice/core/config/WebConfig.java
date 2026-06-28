package mealyummy.mealservice.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    @Value("${CLIENT_URL:http://localhost:5173}")
    private String clientUrl;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                            "http://localhost:5173",
                            "http://127.0.0.1:5173",
                            frontendUrl,
                            clientUrl,
                            "https://mealyummy.vercel.app"  // Hardcode Vercel domain
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600); // Cache preflight for 1 hour
            }
        };
    }
}
