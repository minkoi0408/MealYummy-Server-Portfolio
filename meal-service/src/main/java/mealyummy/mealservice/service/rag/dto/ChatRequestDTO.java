package mealyummy.mealservice.service.rag.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequestDTO {
    private String question;
    private List<ChatMessageDTO> history;

    @Data
    public static class ChatMessageDTO {
        private String role; // "user" or "model"
        private String content;
    }
}
