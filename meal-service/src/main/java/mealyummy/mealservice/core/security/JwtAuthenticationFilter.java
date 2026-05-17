package mealyummy.mealservice.core.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.service.iam.token.TokenServiceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Filter kiểm tra JWT token trong mỗi request.
 * Phải kiểm tra 2 điều kiện:
 * 1. JWT hợp lệ (chữ ký + chưa hết hạn)
 * 2. Token tồn tại trong Redis (chưa bị logout)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenServiceImpl tokenServiceImpl;



    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                // Lấy toàn bộ Payload (Claims) từ token để bóc tách dữ liệu một lần, tránh parse đi parse lại
                Claims claims = jwtUtil.parseClaims(token);

                String username = claims.getSubject();
                String tokenType = (String) claims.get("type");

                // Chỉ chấp nhận Access Token cho các request API và phải tồn tại hợp lệ trong Redis
                boolean isValid = "ACCESS".equals(tokenType)
                        && tokenServiceImpl.isAccessTokenValid(username, token);

                if (isValid) {
                    List<GrantedAuthority> authorities = new ArrayList<>();

                    // 1. Bóc tách mảng "permissions" từ JWT Claim và chuyển thành SimpleGrantedAuthority
                    List<?> rawPermissions = claims.get("permissions", List.class);
                    if (rawPermissions != null) {
                        rawPermissions.forEach(permission -> {
                            authorities.add(new SimpleGrantedAuthority(permission.toString()));
                        });
                    }

                    // 2. Bóc tách "role" (Ví dụ: "ROLE_ADMIN") và đưa vào authorities nếu cần dùng hasRole()
                    String role = (String) claims.get("role");
                    if (role != null && !role.trim().isEmpty()) {
                        authorities.add(new SimpleGrantedAuthority(role));
                    }

                    // 3. Nạp danh sách quyền (authorities) vừa lấy được vào Token xác thực thay vì Collections.emptyList()
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    // Kích hoạt thông tin xác thực vào hệ thống Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
