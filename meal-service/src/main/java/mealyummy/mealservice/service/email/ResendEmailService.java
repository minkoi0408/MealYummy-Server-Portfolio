package mealyummy.mealservice.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ResendEmailService {

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    private final RestTemplate restTemplate;

    public ResendEmailService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            String url = "https://api.resend.com/emails";

            // Build request body
            Map<String, Object> request = new HashMap<>();
            request.put("from", fromEmail);
            request.put("to", new String[]{to});
            request.put("subject", subject);
            request.put("html", htmlContent);

            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            // Send request
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email sent successfully to {} via Resend", to);
            } else {
                log.error("❌ Resend API returned status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to send email via Resend");
            }

        } catch (Exception e) {
            log.error("❌ Error sending email via Resend to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
