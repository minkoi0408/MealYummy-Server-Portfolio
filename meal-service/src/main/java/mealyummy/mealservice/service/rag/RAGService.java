package mealyummy.mealservice.service.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import mealyummy.mealservice.model.entity.Meal;
import mealyummy.mealservice.model.repository.MealRepository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RAGService {

    private final MongoTemplate mongoTemplate;
    private final MealRepository mealRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    public String ingestMedicalKnowledge(MultipartFile file) {
        try {
            // Xóa sạch dữ liệu cũ nhưng GIỮ LẠI Collection và Index
            mongoTemplate.remove(new org.springframework.data.mongodb.core.query.Query(), "vector_store");

            // 1. Đọc nội dung file Text
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 2. Băm nhỏ văn bản (Mỗi đoạn khoảng 500 ký tự)
            List<String> chunks = chunkText(content, 500);

            // 3. Gọi Gemini API để lấy Vector (Embedding) và lưu vào MongoDB
            int savedCount = 0;
            for (String chunk : chunks) {
                if (chunk.trim().isEmpty()) continue;
                
                List<Double> embedding = getEmbedding(chunk);
                
                if (embedding != null && !embedding.isEmpty()) {
                    // Tạo Document để lưu vào collection "vector_store"
                    Document doc = new Document("_id", UUID.randomUUID().toString())
                            .append("text", chunk)
                            .append("embedding", embedding)
                            .append("source", file.getOriginalFilename());
                            
                    mongoTemplate.getCollection("vector_store").insertOne(doc);
                    savedCount++;
                }
            }

            return "Đã nạp thành công " + savedCount + " đoạn kiến thức y khoa vào Không gian Vector (MongoDB)!";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi nạp tài liệu: " + e.getMessage());
        }
    }

    /**
     * Hàm gọi API Gemini thủ công để lấy Vector
     */
    private List<Double> getEmbedding(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Body request theo chuẩn của Gemini Embedding
        String requestBody = "{\"model\": \"models/gemini-embedding-001\", \"content\": {\"parts\": [{\"text\": \"" 
                             + text.replace("\"", "\\\"").replace("\n", " ") + "\"}]}}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("embedding")) {
                Map<String, Object> embeddingObj = (Map<String, Object>) body.get("embedding");
                List<Double> values = (List<Double>) embeddingObj.get("values");
                // Cắt vector xuống đúng 768 chiều để khớp với Index của MongoDB Atlas
                if (values.size() > 768) {
                    return new ArrayList<>(values.subList(0, 768));
                }
                return values;
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini Embedding API: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Hàm chia nhỏ văn bản đơn giản
     */
    private List<String> chunkText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        for (int i = 0; i < length; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(length, i + chunkSize)));
        }
        return chunks;
    }

    /**
     * TÌM KIẾM VECTOR & TRẢ LỜI CÂU HỎI BẰNG GEMINI CHAT
     */
    public String askQuestion(String question) {
        // Bước 1: Biến câu hỏi thành Vector
        List<Double> questionEmbedding = getEmbedding(question);
        if (questionEmbedding == null || questionEmbedding.isEmpty()) {
            return "Xin lỗi, hệ thống AI đang gặp sự cố khi xử lý câu hỏi của bạn.";
        }

        // Bước 2: Tìm kiếm Vector tương đồng trong MongoDB
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
            return "Hệ thống cơ sở dữ liệu y khoa đang bảo trì, vui lòng thử lại sau.";
        }

        String context = contextBuilder.toString().trim();
        if (context.isEmpty()) {
            return "Xin lỗi, tôi chưa được học kiến thức nào liên quan đến vấn đề này. Hãy cung cấp thêm thông tin.";
        }

        // Bước 3: Lấy danh sách món ăn từ Menu để AI gợi ý
        List<Meal> meals = mealRepository.findAll();
        // Lấy ngẫu nhiên khoảng 10 món để tránh vượt quá giới hạn độ dài Prompt
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

        // Bước 4: Tạo Prompt siêu to khổng lồ
        String prompt = "Bạn là Bác sĩ Dinh dưỡng AI cực kỳ thông minh, thân thiện và tận tâm của nhà hàng MealYummy.\n" +
                        "Nhiệm vụ của bạn:\n" +
                        "1. Nếu người dùng cung cấp chiều cao và cân nặng, HÃY tự động tính chỉ số BMI, đánh giá tình trạng cơ thể (thiếu cân, bình thường, thừa cân, béo phì) và đưa ra lời nhận xét thấu cảm, động viên họ.\n" +
                        "2. Dựa CHỦ YẾU VÀO các thông tin y khoa được cung cấp bên dưới, hãy giải đáp câu hỏi của họ một cách chuyên nghiệp, dễ hiểu.\n" +
                        "3. BẮT BUỘC HÃY GỢI Ý 1-2 món ăn phù hợp nhất từ MENU CỦA NHÀ HÀNG dưới đây (Tuyệt đối không bịa ra món mới). KHI GỢI Ý, hãy ghi chú đầy đủ thông tin dinh dưỡng (Calo, Protein, Fat, Carbs) để khách hàng tham khảo.\n\n" +
                        "--- THÔNG TIN Y KHOA (RAG CONTEXT) ---\n" +
                        context + "\n" +
                        "------------------------\n\n" +
                        "--- MENU NHÀ HÀNG MEALYUMMY ---\n" +
                        menuBuilder.toString() + "\n" +
                        "------------------------\n\n" +
                        "CÂU HỎI CỦA NGƯỜI DÙNG: " + question;

        // Bước 5: Gửi cho Gemini Chat API
        return generateChatResponse(prompt);
    }

    /**
     * Gọi Gemini Chat API thủ công (GenerateContent)
     */
    private String generateChatResponse(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");
        String requestBody = "{\"contents\": [{\"parts\": [{\"text\": \"" + safePrompt + "\"}]}]}";
        
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
