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

    public String askQuestion(mealyummy.mealservice.service.rag.dto.ChatRequestDTO request, String username) {
        String question = request.getQuestion();
        
        List<Double> questionEmbedding = getEmbedding(question);
        if (questionEmbedding == null || questionEmbedding.isEmpty()) {
            return "Xin lỗi, hệ thống AI đang gặp sự cố khi xử lý câu hỏi của bạn.";
        }

        List<Document> pipeline = List.of(
            new Document("$vectorSearch",
                new Document("index", "vector_index")
                    .append("path", "embedding")
                    .append("queryVector", questionEmbedding)
                    .append("numCandidates", 50)
                    .append("limit", 3)
            ),
            new Document("$project",
                new Document("text", 1)
                    .append("score", new Document("$meta", "vectorSearchScore"))
            )
        );

        StringBuilder contextBuilder = new StringBuilder();
        try {
            com.mongodb.client.AggregateIterable<Document> results = mongoTemplate.getCollection("vector_store").aggregate(pipeline);
            for (Document doc : results) {
                contextBuilder.append("- ").append(doc.getString("text")).append("\n\n");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi tìm kiếm Vector trong MongoDB: " + e.getMessage());
            return "Xin lỗi, hệ thống cơ sở dữ liệu y khoa đang bảo trì, vui lòng thử lại sau.";
        }

        String context = contextBuilder.toString().trim();
        if (context.isEmpty()) {
            return "Xin lỗi, tôi chưa được học kiến thức nào liên quan đến vấn đề này. Hãy cung cấp thêm thông tin.";
        }

        List<Meal> meals = mealRepository.findAll();
        if (meals.size() > 10) {
            java.util.Collections.shuffle(meals);
            meals = meals.subList(0, 10);
        }
        
        StringBuilder menuBuilder = new StringBuilder();
        for (Meal meal : meals) {
            menuBuilder.append("- Món: ").append(meal.getName())
                       .append(" (").append(meal.getDescription()).append(")\n");
            if (meal.getNutrition() != null) {
                menuBuilder.append("  [Calo: ").append(meal.getNutrition().getCalories())
                           .append(", Protein: ").append(meal.getNutrition().getProtein()).append("g")
                           .append(", Fat: ").append(meal.getNutrition().getFat()).append("g")
                           .append(", Carbs: ").append(meal.getNutrition().getCarbs()).append("g]\n");
            }
            if (meal.getImages() != null && !meal.getImages().isEmpty()) {
                menuBuilder.append("  URL_Image: ").append(meal.getImages().get(0).getUrl()).append("\n");
            }
        }

        String systemInstruction = "Bạn là Bác sĩ Dinh dưỡng AI cực kỳ thông minh, thân thiện và tận tâm của nhà hàng MealYummy.\n" +
                        "Nhiệm vụ của bạn:\n" +
                        "1. Nếu người dùng cung cấp chiều cao và cân nặng, HÃY tự động tính chỉ số BMI, đánh giá tình trạng cơ thể (thiếu cân, bình thường, thừa cân, béo phì) và đưa ra lời nhận xét thấu cảm, động viên họ.\n" +
                        "2. Dựa CHỦ YẾU VÀO các thông tin y khoa được cung cấp bên dưới, hãy giải đáp câu hỏi của họ một cách chuyên nghiệp, dễ hiểu.\n" +
                        "3. BẮT BUỘC HÃY GỢI Ý 1-2 món ăn phù hợp nhất từ MENU CỦA NHÀ HÀNG dưới đây (Tuyệt đối không bịa ra món mới). KHI GỢI Ý, hãy ghi chú đầy đủ thông tin dinh dưỡng (Calo, Protein, Fat, Carbs) để khách hàng tham khảo.\n\n" +
                        "--- THÔNG TIN Y KHOA (RAG CONTEXT) ---\n" +
                        context + "\n" +
                        "------------------------\n\n" +
                        "--- MENU NHÀ HÀNG MEALYUMMY ---\n" +
                        menuBuilder.toString();

        String answer = generateChatResponse(systemInstruction, request.getHistory(), question);

        if (username != null && !answer.contains("quá tải hoặc không phản hồi") && !answer.contains("Xin lỗi")) {
            AiChatSession session = aiChatSessionRepository.findById(username).orElse(new AiChatSession(username, new ArrayList<>(), Instant.now()));
            List<AiChatSession.ChatMessage> history = new ArrayList<>();
            if (request.getHistory() != null) {
                for (var dto : request.getHistory()) {
                    history.add(new AiChatSession.ChatMessage(dto.getRole(), dto.getContent()));
                }
            }
            history.add(new AiChatSession.ChatMessage("user", question));
            history.add(new AiChatSession.ChatMessage("model", answer));
            
            session.setMessages(history);
            session.setUpdatedAt(Instant.now());
            aiChatSessionRepository.save(session);
        }

        return answer;
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
