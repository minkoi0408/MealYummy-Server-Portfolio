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

    public String syncMealEmbeddings() {
        List<Meal> meals = mealRepository.findByActiveTrue();
        int count = 0;
        for (Meal meal : meals) {
            StringBuilder textToEmbed = new StringBuilder();
            if (meal.getName() != null) textToEmbed.append(meal.getName()).append(" ");
            if (meal.getDescription() != null) textToEmbed.append(meal.getDescription()).append(" ");
            
            if (meal.getCategories() != null) {
                for (String cat : meal.getCategories()) {
                    if (cat != null) textToEmbed.append(cat).append(" ");
                }
            }
            if (meal.getTags() != null) {
                for (String tag : meal.getTags()) {
                    if (tag != null) textToEmbed.append(tag).append(" ");
                }
            }
            
            String content = textToEmbed.toString().trim();
            if (!content.isEmpty()) {
                List<Double> embedding = getEmbedding(content);
                if (embedding != null) {
                    meal.setEmbedding(embedding);
                    mealRepository.save(meal);
                    count++;
                }
            }
        }
        return "Đã đồng bộ Vector (Embedding) thành công cho " + count + " món ăn.";
    }

    public Object askQuestion(mealyummy.mealservice.service.rag.dto.ChatRequestDTO request, String username) {
        String question = request.getQuestion();
        
        // 1. Convert user question to vector
        List<Double> questionEmbedding = getEmbedding(question);
        
        List<Meal> responseList = new ArrayList<>();
        
        if (questionEmbedding != null) {
            // 2. Perform MongoDB Atlas Vector Search
            // Note: This requires an Atlas Vector Search index named "vector_index" on the "meals" collection
            org.springframework.data.mongodb.core.aggregation.Aggregation aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                (org.springframework.data.mongodb.core.aggregation.AggregationOperationContext context) -> new Document("$vectorSearch", new Document("index", "vector_index")
                        .append("path", "embedding")
                        .append("queryVector", questionEmbedding)
                        .append("numCandidates", 100)
                        .append("limit", 12)
                ),
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                    org.springframework.data.mongodb.core.query.Criteria.where("active").is(true)
                )
            );

            org.springframework.data.mongodb.core.aggregation.AggregationResults<Meal> results = 
                mongoTemplate.aggregate(aggregation, "meals_v2", Meal.class);
                
            responseList = results.getMappedResults();
        }
        
        // Fallback or if Vector Search failed (or no index)
        if (responseList.isEmpty()) {
             // Fallback to basic keyword search or return top active meals
             org.springframework.data.mongodb.core.query.Query fallbackQuery = new org.springframework.data.mongodb.core.query.Query(
                 org.springframework.data.mongodb.core.query.Criteria.where("active").is(true)
             ).limit(12);
             responseList = mongoTemplate.find(fallbackQuery, Meal.class, "meals_v2");
        }

        // 3. Format response using Gemini for natural language (Optional but requested)
        StringBuilder mealsContext = new StringBuilder();
        for (Meal meal : responseList) {
            mealsContext.append("- ").append(meal.getName()).append(": ").append(meal.getDescription()).append("\n");
        }
        
        String systemInstruction = "Bạn là chuyên gia dinh dưỡng AI. Dựa vào danh sách các món ăn đã được tìm kiếm dưới đây:\n"
            + mealsContext.toString() + "\n\n"
            + "Hãy trả lời câu hỏi của người dùng và tư vấn lý do tại sao các món này lại phù hợp một cách ngắn gọn, tự nhiên.";
            
        String aiChatResponse = generateChatResponse(systemInstruction, request.getHistory(), question);

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
            history.add(new AiChatSession.ChatMessage("model", aiChatResponse));
            
            session.setMessages(history);
            session.setUpdatedAt(Instant.now());
            aiChatSessionRepository.save(session);
        }

        // Return a custom object containing both the AI text and the list of meals
        return Map.of(
            "aiMessage", aiChatResponse,
            "suggestedMeals", responseList
        );
    }

    private String generateChatResponse(String systemInstruction, List<mealyummy.mealservice.service.rag.dto.ChatRequestDTO.ChatMessageDTO> history, String currentQuestion) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-latest:generateContent?key=" + geminiApiKey;
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
