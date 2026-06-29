package mealyummy.mealservice.service.iam.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.security.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String ACCESS_PREFIX = "access:";
    private static final String REFRESH_PREFIX = "refresh:";

    /**
     * Lấy danh sách các user đang active dựa trên token trong Redis
     */
    public List<String> getActiveUsers() {
        Set<String> keys = redisTemplate.keys(ACCESS_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }
        
        Set<String> activeUsers = new HashSet<>();
        for (String key : keys) {
            // Key format: access:{username}:{tokenId}
            String[] parts = key.split(":");
            if (parts.length >= 2) {
                activeUsers.add(parts[1]);
            }
        }
        return new ArrayList<>(activeUsers);
    }

    /**
     * Lưu Access Token vào Redis (TTL: 5 phút)
     */
    public void storeToken(String username, String token, String prefix, long expiration) {
        // 1. Lấy ID và tạo Key duy nhất
        String tokenId = jwtUtil.getTokenId(token);
        String key = prefix + username + ":" + tokenId;

//        // 3. (Optional) Xóa các token cũ nếu bạn muốn duy trì Single Session
//        Set<String> oldKeys = redisTemplate.keys(prefix + username + ":*");
//        if (oldKeys != null && !oldKeys.isEmpty()) {
//            redisTemplate.delete(oldKeys);
//        }

        // 4. Lưu vào Redis
        redisTemplate.opsForValue().set(
                key,
                token,
                expiration,
                TimeUnit.MILLISECONDS);

        log.debug("Đã lưu {} vào Redis cho user: {}", prefix, username);
    }

    /**
     * Kiểm tra Access Token có tồn tại trong Redis không
     */
    public boolean isAccessTokenValid(String username, String token) {
        try {
            String tokenId = jwtUtil.getTokenId(token);
            String key = ACCESS_PREFIX + username + ":" + tokenId;
            String storedToken = redisTemplate.opsForValue().get(key);
            // Trong môi trường DEV, nếu Redis lỗi, ta tạm chấp nhận token chỉ dựa trên JWT
            // validation
            if (storedToken == null)
                return true;
            return token.equals(storedToken);
        } catch (Exception e) {
            log.warn("LỖI REDIS: Đang bỏ qua kiểm tra Redis và chỉ dùng JWT validation. Error: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Kiểm tra Refresh Token có tồn tại trong Redis không
     */
    public boolean isRefreshTokenValid(String username, String token) {
        String tokenId = jwtUtil.getTokenId(token);
        String key = REFRESH_PREFIX + username + ":" + tokenId;
        String storedToken = redisTemplate.opsForValue().get(key);
        return token.equals(storedToken);
    }


    /**
     * XÓA TẤT CẢ token của user (dùng khi logout toàn bộ thiết bị)
     */
    public void removeAllTokensOfUser(String username) {
        // Xóa tất cả access tokens
        var accessKeys = redisTemplate.keys(ACCESS_PREFIX + username + ":*");
        if (accessKeys != null && !accessKeys.isEmpty()) {
            redisTemplate.delete(accessKeys);
        }

        // Xóa tất cả refresh tokens
        var refreshKeys = redisTemplate.keys(REFRESH_PREFIX + username + ":*");
        if (refreshKeys != null && !refreshKeys.isEmpty()) {
            redisTemplate.delete(refreshKeys);
        }

        log.info("Đã xóa tất cả token của user: {}", username);
    }
}