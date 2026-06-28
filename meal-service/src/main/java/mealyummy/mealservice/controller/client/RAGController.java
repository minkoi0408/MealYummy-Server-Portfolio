package mealyummy.mealservice.controller.client;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.rag.RAGService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import mealyummy.mealservice.service.rag.dto.ChatRequestDTO;
import mealyummy.mealservice.core.annotation.RateLimit;

@RestController
@RequestMapping("/api/v1/ai/knowledge")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @PostMapping(value = "/ingest", consumes = "multipart/form-data")
    public ResponseEntity<BaseApiResponse<String>> ingestKnowledge(@RequestParam("file") MultipartFile file) {
        String msg = ragService.ingestMedicalKnowledge(file);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @PostMapping("/sync-meals")
    public ResponseEntity<BaseApiResponse<String>> syncMeals() {
        String msg = ragService.syncMealEmbeddings();
        return ResponseEntity.ok(BaseApiResponse.ok(msg, null));
    }

    @PostMapping("/chat")
    @RateLimit(key = "ai_prompt", freeLimit = 10, membershipLimit = 50)
    public ResponseEntity<BaseApiResponse<Object>> chatWithAI(@RequestBody ChatRequestDTO request, java.security.Principal principal) {
        String username = principal != null ? principal.getName() : null;
        Object answer = ragService.askQuestion(request, username);
        return ResponseEntity.ok(BaseApiResponse.ok("Thành công", answer));
    }

    @GetMapping("/restaurant-keywords")
    public ResponseEntity<BaseApiResponse<java.util.List<String>>> getRestaurantKeywords(@RequestParam String mealName) {
        java.util.List<String> keywords = ragService.getRestaurantKeywords(mealName);
        return ResponseEntity.ok(BaseApiResponse.ok("Thành công", keywords));
    }

    @GetMapping("/quota")
    public ResponseEntity<BaseApiResponse<java.util.Map<String, Integer>>> getQuota(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(BaseApiResponse.ok("Guest", java.util.Map.of("limit", 0, "used", 0, "remaining", 0)));
        }
        String username = auth.getName();
        java.util.Set<String> roles = auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
                
        int limit = 10; // freeLimit
        if (roles.contains("ROLE_ADMIN")) {
            limit = 9999;
        } else if (roles.contains("ROLE_MEMBERSHIP")) {
            limit = 50; // membershipLimit
        }
        
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        String redisKey = String.format("rate_limit:ai_prompt:%s:%s", username, today);
        String currentCountStr = redisTemplate.opsForValue().get(redisKey);
        int used = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;
        
        return ResponseEntity.ok(BaseApiResponse.ok("Thành công", java.util.Map.of("limit", limit, "used", used, "remaining", Math.max(0, limit - used))));
    }

    @GetMapping("/chat/history")
    public ResponseEntity<BaseApiResponse<java.util.List<ChatRequestDTO.ChatMessageDTO>>> getChatHistory(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(BaseApiResponse.ok("Guest", new java.util.ArrayList<>()));
        }
        java.util.List<ChatRequestDTO.ChatMessageDTO> history = ragService.getChatHistory(principal.getName());
        return ResponseEntity.ok(BaseApiResponse.ok("Thành công", history));
    }

    @DeleteMapping("/chat/history")
    public ResponseEntity<BaseApiResponse<String>> clearChatHistory(java.security.Principal principal) {
        if (principal != null) {
            ragService.clearChatHistory(principal.getName());
        }
        return ResponseEntity.ok(BaseApiResponse.ok("Thành công", "Đã xóa lịch sử"));
    }
}
