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
        
        // 1. Fetch raw documents to bypass Spring Data Lazy Loading N+1 queries completely
        org.springframework.data.mongodb.core.query.Query mealQuery = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("active").is(true)
        );
        // Exclude large fields to save memory and tokens
        mealQuery.fields().include("_id", "name", "description", "categories", "tags");
        
        List<Document> rawMeals = mongoTemplate.find(mealQuery, Document.class, "meals");
        List<Document> rawCategories = mongoTemplate.findAll(Document.class, "categories");
        List<Document> rawTags = mongoTemplate.findAll(Document.class, "tags");

        // Format references efficiently
        List<Map<String, Object>> catalogMeals = new ArrayList<>();
        for (Document doc : rawMeals) {
            Map<String, Object> simpleMeal = new java.util.HashMap<>();
            simpleMeal.put("id", doc.getObjectId("_id").toHexString());
            simpleMeal.put("name", doc.getString("name"));
            simpleMeal.put("description", doc.getString("description"));
            
            // Extract category and tag IDs
            Object catObj = doc.get("categories");
            if (catObj instanceof List) {
                List<String> cIds = new ArrayList<>();
                for (Object c : (List<?>) catObj) {
                    if (c instanceof org.bson.types.ObjectId) cIds.add(((org.bson.types.ObjectId) c).toHexString());
                    else if (c instanceof com.mongodb.DBRef) cIds.add(((com.mongodb.DBRef) c).getId().toString());
                    else cIds.add(c.toString());
                }
                simpleMeal.put("categoryIds", cIds);
            }
            
            Object tagObj = doc.get("tags");
            if (tagObj instanceof List) {
                List<String> tIds = new ArrayList<>();
                for (Object t : (List<?>) tagObj) {
                    if (t instanceof org.bson.types.ObjectId) tIds.add(((org.bson.types.ObjectId) t).toHexString());
                    else if (t instanceof com.mongodb.DBRef) tIds.add(((com.mongodb.DBRef) t).getId().toString());
                    else tIds.add(t.toString());
                }
                simpleMeal.put("tagIds", tIds);
            }
            catalogMeals.add(simpleMeal);
        }

        List<Map<String, String>> catalogCategories = new ArrayList<>();
        for (Document doc : rawCategories) {
            catalogCategories.add(Map.of("id", doc.getObjectId("_id").toHexString(), "name", doc.getString("name")));
        }

        List<Map<String, String>> catalogTags = new ArrayList<>();
        for (Document doc : rawTags) {
            catalogTags.add(Map.of("id", doc.getObjectId("_id").toHexString(), "name", doc.getString("name")));
        }

        // Convert catalog to JSON String
        String catalogJson = "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> fullCatalog = Map.of(
                "categories", catalogCategories,
                "tags", catalogTags,
                "meals", catalogMeals
            );
            catalogJson = mapper.writeValueAsString(fullCatalog);
        } catch(Exception e) {}

        // 2. Pass to LLM for semantic search
        String systemInstruction = "Bạn là chuyên gia dinh dưỡng AI. Dưới đây là danh sách danh mục (categories), thẻ (tags) và danh sách toàn bộ món ăn (meals) của hệ thống:\n" 
            + catalogJson + "\n\n"
            + "Dựa vào yêu cầu của người dùng, hãy chọn ra tối đa 10 món ăn phù hợp nhất về mặt ngữ nghĩa, dinh dưỡng và mục tiêu. Nếu người dùng chọn tag hay category, hãy phân tích từ điển tags/categories để ánh xạ với các tagIds/categoryIds của từng món ăn.\n"
            + "CHỈ trả về ĐÚNG 1 mảng JSON chứa các ID của món ăn được chọn (Ví dụ: [\"6a21...\", \"6a22...\"]). KHÔNG BAO GỒM BẤT KỲ VĂN BẢN HAY CODE BLOCK MARKDOWN NÀO KHÁC.";

        String responseJson = generateChatResponse(systemInstruction, request.getHistory(), question);

        List<String> selectedIds = new ArrayList<>();
        try {
            String cleanJson = responseJson.replaceAll("(?s)```json(.*?)```", "$1").replaceAll("(?s)```(.*?)```", "$1").trim();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            selectedIds = mapper.readValue(cleanJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON từ LLM: " + responseJson);
        }

        // 3. Lấy kết quả DTO
        List<Object> responseList = new ArrayList<>();
        for (String id : selectedIds) {
            try {
                responseList.add(mealService.get(id));
            } catch(Exception e) {}
        }

        // Fallback ngẫu nhiên nếu LLM lỗi
        if (responseList.isEmpty()) {
            int maxFallback = Math.min(10, catalogMeals.size());
            for (int i = 0; i < maxFallback; i++) {
                try {
                    responseList.add(mealService.get(catalogMeals.get(i).get("id").toString()));
                } catch(Exception e) {}
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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;
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
