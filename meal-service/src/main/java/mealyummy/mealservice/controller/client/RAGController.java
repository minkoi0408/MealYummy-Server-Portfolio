package mealyummy.mealservice.controller.client;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.rag.RAGService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import mealyummy.mealservice.service.rag.dto.ChatRequestDTO;

@RestController
@RequestMapping("/api/v1/ai/knowledge")
@RequiredArgsConstructor
public class RAGController {

    private final RAGService ragService;

    @PostMapping(value = "/ingest", consumes = "multipart/form-data")
    public ResponseEntity<BaseApiResponse<String>> ingestKnowledge(@RequestParam("file") MultipartFile file) {
        String msg = ragService.ingestMedicalKnowledge(file);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok(msg, null));
    }

    @PostMapping("/chat")
    public ResponseEntity<BaseApiResponse<String>> chatWithAI(@RequestBody ChatRequestDTO request, java.security.Principal principal) {
        String username = principal != null ? principal.getName() : null;
        String answer = ragService.askQuestion(request, username);
        return ResponseEntity.ok(BaseApiResponse.ok("Thành công", answer));
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
