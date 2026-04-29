package mealyummy.mealservice.service.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.security.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service quản lý token thông qua Redis.
 *
 * Cách hoạt động:
 * - Khi login: lưu Access Token và Refresh Token vào Redis với TTL tương ứng
 * - Khi gọi API: kiểm tra Access Token có tồn tại trong Redis không
 * - Khi refresh: xóa Access Token cũ, tạo Access Token mới
 * - Khi logout: xóa cả Access Token và Refresh Token khỏi Redis
 *
 * Redis Key Format:
 * - Access Token:  "access:{username}:{tokenId}"
 * - Refresh Token: "refresh:{username}:{tokenId}"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String ACCESS_PREFIX = "access:";
    private static final String REFRESH_PREFIX = "refresh:";

    /**
     * Lưu Access Token vào Redis (TTL: 5 phút)
     */
    public void storeAccessToken(String username, String token) {
        String tokenId = jwtUtil.getTokenId(token);
        String key = ACCESS_PREFIX + username + ":" + tokenId;

        redisTemplate.opsForValue().set(key, token,
                jwtUtil.getAccessTokenExpirationMs(), TimeUnit.MILLISECONDS);

        log.debug("Lưu Access Token vào Redis: {}", key);
    }

    /**
     * Lưu Refresh Token vào Redis (TTL: 1 tuần)
     */
    public void storeRefreshToken(String username, String token) {
        String tokenId = jwtUtil.getTokenId(token);
        String key = REFRESH_PREFIX + username + ":" + tokenId;

        redisTemplate.opsForValue().set(key, token,
                jwtUtil.getRefreshTokenExpirationMs(), TimeUnit.MILLISECONDS);

        log.debug("Lưu Refresh Token vào Redis: {}", key);
    }

    /**
     * Kiểm tra Access Token có tồn tại trong Redis không
     */
    public boolean isAccessTokenValid(String username, String token) {
        String tokenId = jwtUtil.getTokenId(token);
        String key = ACCESS_PREFIX + username + ":" + tokenId;
        String storedToken = redisTemplate.opsForValue().get(key);
        return token.equals(storedToken);
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
     * Xóa một Access Token cụ thể
     */
    public void removeAccessToken(String username, String token) {
        String tokenId = jwtUtil.getTokenId(token);
        String key = ACCESS_PREFIX + username + ":" + tokenId;
        redisTemplate.delete(key);
        log.debug("Xóa Access Token khỏi Redis: {}", key);
    }

    /**
     * Xóa một Refresh Token cụ thể
     */
    public void removeRefreshToken(String username, String token) {
        String tokenId = jwtUtil.getTokenId(token);
        String key = REFRESH_PREFIX + username + ":" + tokenId;
        redisTemplate.delete(key);
        log.debug("Xóa Refresh Token khỏi Redis: {}", key);
    }

    /**
     * Xóa TẤT CẢ token của user (dùng khi logout toàn bộ thiết bị)
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
