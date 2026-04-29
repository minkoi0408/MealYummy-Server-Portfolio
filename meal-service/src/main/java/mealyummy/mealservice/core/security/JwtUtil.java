package mealyummy.mealservice.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
     * Tạo Access Token (sống 5 phút)
     */
    public String generateAccessToken(String username) {
        return buildToken(username, accessTokenExpirationMs, "ACCESS");
    }

    /**
     * Tạo Refresh Token (sống 1 tuần)
     */
    public String generateRefreshToken(String username) {
        return buildToken(username, refreshTokenExpirationMs, "REFRESH");
    }

    /**
     * Tạo token với type (ACCESS hoặc REFRESH)
     */
    private String buildToken(String username, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("type", tokenType)
                .id(UUID.randomUUID().toString()) // unique token ID
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
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

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
