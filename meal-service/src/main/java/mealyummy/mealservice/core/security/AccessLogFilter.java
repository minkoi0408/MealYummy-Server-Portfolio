package mealyummy.mealservice.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.entity.log.AccessLog;
import mealyummy.mealservice.service.log.AccessLogService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {

    private final AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            logAccess(request, response, durationMs);
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        String endpoint = request.getRequestURI();

        // Chỉ log các request bắt đầu bằng /api/ và bỏ qua một số path như swagger, healthcheck, ...
        if (endpoint == null || !endpoint.startsWith("/api/") || endpoint.contains("/api/v1/dashboard/access-logs")) {
            return;
        }

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = "ANONYMOUS";
            String userId = null; // JWT username actually contains userId in this system, but let's just log what we have
            
            if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
                username = authentication.getName(); 
                userId = authentication.getName(); // System uses user ID as username in JWT
            }

            String ipAddress = getClientIp(request);
            String method = request.getMethod();
            String userAgent = request.getHeader("User-Agent");
            int status = response.getStatus();

            AccessLog accessLog = AccessLog.builder()
                    .userId(userId)
                    .username(username)
                    .ipAddress(ipAddress)
                    .endpoint(endpoint)
                    .method(method)
                    .userAgent(userAgent)
                    .status(status)
                    .durationMs(durationMs)
                    .createdAt(LocalDateTime.now())
                    .build();

            accessLogService.saveLogAsync(accessLog);

        } catch (Exception e) {
            log.error("Lỗi khi ghi log truy cập: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}