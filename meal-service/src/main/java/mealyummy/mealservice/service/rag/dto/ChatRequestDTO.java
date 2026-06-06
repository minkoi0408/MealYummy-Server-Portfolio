package mealyummy.mealservice.service.rag.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequestDTO {
    private String question;
    private List<ChatMessageDTO> history;
    private List<String> categoryIds;
    private List<String> tagIds;
    private List<String> excludedIngredientIds;

    @Data
    public static class ChatMessageDTO {
        private String role; // "user" or "model"
        private String content;
    }
}
