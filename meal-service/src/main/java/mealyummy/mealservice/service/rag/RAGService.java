package mealyummy.mealservice.service.rag;

import lombok.RequiredArgsConstructor;
import mealyummy.mealservice.model.entity.food.Meal;
import mealyummy.mealservice.model.entity.AiChatSession;
import mealyummy.mealservice.model.repository.MealRepository;
import mealyummy.mealservice.model.repository.AiChatSessionRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RAGService {

    private final MongoTemplate mongoTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final MealRepository mealRepository;
    private final AiChatSessionRepository aiChatSessionRepository;
    private final mealyummy.mealservice.service.meal.MealService mealService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public List<mealyummy.mealservice.service.rag.dto.ChatRequestDTO.ChatMessageDTO> getChatHistory(String username) {
        return aiChatSessionRepository.findById(username)
                .map(session -> session.getMessages().stream()
                        .map(msg -> {
                            mealyummy.mealservice.service.rag.dto.ChatRequestDTO.ChatMessageDTO dto = new mealyummy.mealservice.service.rag.dto.ChatRequestDTO.ChatMessageDTO();
                            dto.setRole(msg.getRole());
                            dto.setContent(msg.getContent());
                            return dto;
                        })
                        .toList())
                .orElse(new ArrayList<>());
    }

    public void clearChatHistory(String username) {
        aiChatSessionRepository.deleteById(username);
    }

    public String ingestMedicalKnowledge(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] chunks = content.split("\n\n"); 
            List<Document> documents = new ArrayList<>();

            for (String chunk : chunks) {
                if (chunk.trim().isEmpty()) continue;
                List<Double> embedding = getEmbedding(chunk.trim());
                if (embedding != null) {
                    Document doc = new Document("text", chunk.trim())
                            .append("embedding", embedding);
                    documents.add(doc);
                }
            }

            if (!documents.isEmpty()) {
                mongoTemplate.getCollection("vector_store").insertMany(documents);
                return "Đã nạp " + documents.size() + " đoạn kiến thức y khoa thành công!";
            }
            return "File không có nội dung hợp lệ.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi khi xử lý file: " + e.getMessage();
        }
    }

    private List<Double> getEmbedding(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String safeText = text.replace("\"", "\\\"").replace("\n", "\\n");
        String requestBody = "{\"model\": \"models/gemini-embedding-001\", \"content\": {\"parts\": [{\"text\": \"" + safeText + "\"}]}, \"outputDimensionality\": 768}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("embedding")) {
                Map<String, Object> embeddingNode = (Map<String, Object>) body.get("embedding");
                return (List<Double>) embeddingNode.get("values");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini Embedding API: " + e.getMessage());
        }
        return null;
    }

    static class MealScore {
        Meal meal;
        int score;
        public MealScore(Meal meal, int score) {
            this.meal = meal;
            this.score = score;
        }
    }

    public Object askQuestion(mealyummy.mealservice.service.rag.dto.ChatRequestDTO request, String username) {
        String question = request.getQuestion();
        
        // 1. Trích xuất tag/category từ prompt người dùng
        String extractPrompt = "Từ câu hỏi sau: '" + question + "'. Hãy trích xuất ra một mảng các từ khóa (tối đa 5 từ) liên quan đến tag, category, hoặc nguyên liệu món ăn. CHỈ trả về đúng 1 mảng JSON các chuỗi (ví dụ: [\"bò\", \"cay\", \"chay\"]), không kèm văn bản nào khác, không dùng markdown.";
        String keywordsJson = generateChatResponse(extractPrompt, null, question);
        
        List<String> keywords = new ArrayList<>();
        try {
            String cleanJson = keywordsJson.replaceAll("(?s)```json(.*?)```", "$1").replaceAll("(?s)```(.*?)```", "$1").trim();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            keywords = mapper.readValue(cleanJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
        } catch (Exception e) {
            keywords.add(question);
        }

        // 2. Tìm kiếm món ăn phù hợp nhất
        List<Meal> allMeals = mealRepository.findAll();
        List<MealScore> mealScores = new ArrayList<>();
        
        for (Meal meal : allMeals) {
            if (!Boolean.TRUE.equals(meal.getActive())) continue;
            
            // Exclusion Check
            if (request.getExcludedIngredientIds() != null && !request.getExcludedIngredientIds().isEmpty() && meal.getIngredients() != null) {
                boolean hasExcluded = false;
                for (var ing : meal.getIngredients()) {
                    if (ing.getIngredientId() != null && request.getExcludedIngredientIds().contains(ing.getIngredientId())) {
                        hasExcluded = true;
                        break;
                    }
                }
                if (hasExcluded) continue;
            }

            // Category Filter Check
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                boolean hasCategory = false;
                if (meal.getCategories() != null) {
                    for (var cat : meal.getCategories()) {
                        if (request.getCategoryIds().contains(cat.getId())) {
                            hasCategory = true;
                            break;
                        }
                    }
                }
                if (!hasCategory) continue;
            }

            // Tag Filter Check
            if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
                boolean hasTag = false;
                if (meal.getTags() != null) {
                    for (var tag : meal.getTags()) {
                        if (request.getTagIds().contains(tag.getId())) {
                            hasTag = true;
                            break;
                        }
                    }
                }
                if (!hasTag) continue;
            }

            int score = 0;
            String nameLower = meal.getName() != null ? meal.getName().toLowerCase() : "";
            String descLower = meal.getDescription() != null ? meal.getDescription().toLowerCase() : "";
            
            for (String kw : keywords) {
                String kLower = kw.toLowerCase();
                if (nameLower.contains(kLower)) score += 3;
                if (descLower.contains(kLower)) score += 1;
                
                if (meal.getTags() != null) {
                    for (var t : meal.getTags()) {
                        if (t.getName() != null && t.getName().toLowerCase().contains(kLower)) score += 2;
                    }
                }
                if (meal.getCategories() != null) {
                    for (var c : meal.getCategories()) {
                        if (c.getName() != null && c.getName().toLowerCase().contains(kLower)) score += 2;
                    }
                }
            }
            if (score > 0) {
                mealScores.add(new MealScore(meal, score));
            }
        }
        
        mealScores.sort((a, b) -> Integer.compare(b.score, a.score));
        
        List<Meal> topMeals = mealScores.stream()
                .limit(10)
                .map(ms -> ms.meal)
                .toList();

        if (topMeals.isEmpty()) {
            topMeals = allMeals.stream().limit(10).toList();
        }

        // 3. Map to DTO
        List<Object> responseList = new ArrayList<>();
        for (Meal m : topMeals) {
            // Check if MealService is implemented as MealServiceImpl which contains convertMealToMealResponseDTO
            // Since MealService is an interface, we might not be able to call convertMealToMealResponseDTO if it's not in the interface
            // Let's just return the raw Meal or manually map to a Map to avoid lazy initialization issues.
            // Wait, mealService is interface MealService. Let's cast it or just return Meal.
            // We can fetch via get(id)
            try {
                responseList.add(mealService.get(m.getId()));
            } catch (Exception e) {
                responseList.add(m);
            }
        }

        // Lưu lịch sử chat
        if (username != null) {
            AiChatSession session = aiChatSessionRepository.findById(username).orElse(new AiChatSession(username, new ArrayList<>(), Instant.now()));
            List<AiChatSession.ChatMessage> history = new ArrayList<>();
            if (request.getHistory() != null) {
                for (var dto : request.getHistory()) {
                    history.add(new AiChatSession.ChatMessage(dto.getRole(), dto.getContent()));
                }
            }
            history.add(new AiChatSession.ChatMessage("user", question));
            history.add(new AiChatSession.ChatMessage("model", "Đã gợi ý " + responseList.size() + " món ăn phù hợp với yêu cầu của bạn."));
            
            session.setMessages(history);
            session.setUpdatedAt(Instant.now());
            aiChatSessionRepository.save(session);
        }

        return responseList;
    }

    private String generateChatResponse(String systemInstruction, List<mealyummy.mealservice.service.rag.dto.ChatRequestDTO.ChatMessageDTO> history, String currentQuestion) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        StringBuilder contentsBuilder = new StringBuilder();
        contentsBuilder.append("[");
        
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                mealyummy.mealservice.service.rag.dto.ChatRequestDTO.ChatMessageDTO msg = history.get(i);
                String role = "user".equals(msg.getRole()) ? "user" : "model";
                String safeContent = msg.getContent().replace("\"", "\\\"").replace("\n", "\\n");
                contentsBuilder.append("{\"role\": \"").append(role).append("\", \"parts\": [{\"text\": \"").append(safeContent).append("\"}]}");
                if (i < history.size() - 1) contentsBuilder.append(",");
            }
        }
        
        if (history != null && !history.isEmpty()) contentsBuilder.append(",");
        String safeCurrentQuestion = currentQuestion.replace("\"", "\\\"").replace("\n", "\\n");
        contentsBuilder.append("{\"role\": \"user\", \"parts\": [{\"text\": \"").append(safeCurrentQuestion).append("\"}]}");
        contentsBuilder.append("]");

        String safeSystemInstruction = systemInstruction.replace("\"", "\\\"").replace("\n", "\\n");

        String requestBody = "{" +
                             "\"system_instruction\": {\"parts\": [{\"text\": \"" + safeSystemInstruction + "\"}]}," +
                             "\"contents\": " + contentsBuilder.toString() +
                             "}";
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini Chat API: " + e.getMessage());
        }
        return "Gemini AI hiện đang quá tải hoặc không phản hồi. Vui lòng thử lại sau.";
    }
}
