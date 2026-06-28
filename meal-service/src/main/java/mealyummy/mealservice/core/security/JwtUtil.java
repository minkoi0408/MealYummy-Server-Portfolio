package mealyummy.mealservice.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Tiện ích xử lý JWT Token (tạo, đọc, xác thực)
 * Hỗ trợ 2 loại token: Access Token (5 phút) và Refresh Token (1 tuần)
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Tạo Access Token (Sẽ chứa đầy đủ Role và Permissions)
     */
    public String generateAccessToken(String username, String roleCode, Set<String> permissions) {
        return buildToken(username, roleCode, permissions, accessTokenExpirationMs, "ACCESS");
    }

    /**
     * Tạo Refresh Token (Chỉ chứa Username, KHÔNG chứa Role và Permissions)
     * Hàm này đã được bỏ bớt 2 tham số roleCode và permissions ở đầu vào.
     */
    public String generateRefreshToken(String username) {
        // Truyền null vào các tham số phân quyền
        return buildToken(username, null, null, refreshTokenExpirationMs, "REFRESH");
    }

    /**
     * Tạo token với type (ACCESS hoặc REFRESH)
     */
    private String buildToken(String username, String roleCode, Set<String> permissions, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        // 1. Khởi tạo Builder với các thông tin CƠ BẢN bắt buộc token nào cũng phải có
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .claim("type", tokenType)
                .id(UUID.randomUUID().toString()) // unique token ID
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey);

        // 2. Chỉ nhúng Role nếu có dữ liệu (Dành cho Access Token)
        if (roleCode != null && !roleCode.trim().isEmpty()) {
            builder.claim("role", roleCode);
        }

        // 3. Chỉ nhúng Permissions nếu có dữ liệu (Dành cho Access Token)
        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", permissions);
        }

        // 4. Đóng gói token
        return builder.compact();
    }

    /**
     * Lấy username từ token
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Lấy token type (ACCESS / REFRESH)
     */
    public String getTokenType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    /**
     * Lấy token ID (JTI)
     */
    public String getTokenId(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Kiểm tra token có hợp lệ không (chữ ký + chưa hết hạn)
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
