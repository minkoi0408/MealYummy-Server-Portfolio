package mealyummy.mealservice.controller;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.core.base.BaseApiResponse;
import mealyummy.mealservice.service.rag.RAGService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<BaseApiResponse<String>> chatWithAI(@RequestParam("question") String question) {
        String answer = ragService.askQuestion(question);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(BaseApiResponse.ok("Thành công", answer));
    }
}
