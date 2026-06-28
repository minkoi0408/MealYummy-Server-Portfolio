package mealyummy.mealservice.core.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.annotation.RateLimit;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Set;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu không đăng nhập, từ chối hoặc áp dụng mức free (tuỳ logic hệ thống, ở đây yêu cầu đăng nhập)
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        String username = auth.getName();
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Nếu là ADMIN, không giới hạn
        if (roles.contains("ROLE_ADMIN")) {
            return joinPoint.proceed();
        }

        // Xác định giới hạn dựa trên role
        int limit = rateLimit.freeLimit();
        if (roles.contains("ROLE_MEMBERSHIP")) {
            limit = rateLimit.membershipLimit();
        }

        // Tạo Redis Key theo ngày
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String redisKey = String.format("rate_limit:%s:%s:%s", rateLimit.key(), username, today);

        // Tăng giá trị đếm
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);
        
        // Nếu là lần đầu tiên gọi trong ngày, set thời gian expire
        if (currentCount != null && currentCount == 1L) {
            // Expire sau 24 giờ
            redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);
        }

        // Kiểm tra quá giới hạn
        if (currentCount != null && currentCount > limit) {
            log.warn("Rate limit exceeded for user: {}. API: {}, Count: {}/{}", username, rateLimit.key(), currentCount, limit);
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // Cho phép đi tiếp
        return joinPoint.proceed();
    }
}
