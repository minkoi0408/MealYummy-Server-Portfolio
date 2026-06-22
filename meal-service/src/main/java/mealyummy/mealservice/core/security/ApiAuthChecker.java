package mealyummy.mealservice.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component("apiAuth")
public class ApiAuthChecker {

    public boolean check() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String method = request.getMethod();
        
        // request.getRequestURI() tra ve uri day du (VD: /api/v1/meals), trong khi mapping pattern co the la /api/v1/meals/{id}
        // De ho tro RequestMappingHandlerMapping, cach tot nhat la so khop chinh xac pattern.
        // Tuy nhien, JWT co the co "POST_/api/v1/meals". Ta nen match truc tiep tu request.
        // De don gian, tam thoi chung ta lay "best matching pattern" tu Spring.
        String pattern = (String) request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        
        if (pattern == null) {
            pattern = request.getRequestURI();
        }

        String requiredPermission = method + "_" + pattern;

        // Admin luon duoc phep (Roles co that luu duoi ten ROLE_ADMIN nhung Authorities co full permission roi)
        boolean hasPermission = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(requiredPermission) || a.getAuthority().equals("ROLE_ADMIN"));

        return hasPermission;
    }
}
