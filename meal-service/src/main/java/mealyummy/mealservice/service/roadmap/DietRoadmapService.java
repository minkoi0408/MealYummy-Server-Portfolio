package mealyummy.mealservice.service.roadmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mealyummy.mealservice.core.exception.AppException;
import mealyummy.mealservice.core.exception.ErrorCode;
import mealyummy.mealservice.model.entity.DietRoadmap;
import mealyummy.mealservice.model.entity.auth.User;
import mealyummy.mealservice.model.entity.food.Meal;
import mealyummy.mealservice.model.entity.profile.UserMetrics;
import mealyummy.mealservice.model.pojo.RoadmapPhase;
import mealyummy.mealservice.model.repository.DietRoadmapRepository;
import mealyummy.mealservice.model.repository.MealRepository;
import mealyummy.mealservice.model.repository.UserMetricsRepository;
import mealyummy.mealservice.model.repository.UserRepository;
import mealyummy.mealservice.service.roadmap.dto.DietRoadmapDTO;
import mealyummy.mealservice.service.roadmap.dto.GenerateRoadmapRequest;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietRoadmapService {

    private final UserRepository userRepository;
    private final UserMetricsRepository userMetricsRepository;
    private final MealRepository mealRepository;
    private final DietRoadmapRepository dietRoadmapRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    // ─── Public API ─────────────────────────────────────────────────────────────

    public DietRoadmapDTO generate(String username, GenerateRoadmapRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserMetrics metrics = userMetricsRepository
                .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_METRICS_NOT_FOUND));

        // 1. Tính BMI
        double bmi = calcBmi(metrics.getWeight(), metrics.getHeight());
        String bmiCategory = classifyBmi(bmi);
        String bmiLabel = bmiCategoryLabel(bmiCategory);

        // 2. Lấy RAG context dựa trên bệnh lý
        String ragContext = getRagContext(metrics.getDiseases(), metrics.getGoal());

        // 3. Lấy 15 món ăn random
        String menuContext = buildMenuContext();

        // 4. Build prompt
        String prompt = buildPrompt(metrics, bmi, bmiLabel, ragContext, menuContext, request.getDurationLabel());

        // 5. Gọi Gemini AI
        String aiJson = callGemini(prompt);

        // 6. Parse JSON → entity
        DietRoadmap roadmap = parseAndSave(aiJson, user.getId(), bmi, bmiCategory, bmiLabel, metrics, request.getDurationLabel());

        return toDTO(roadmap);
    }

    public DietRoadmapDTO getCurrent(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return dietRoadmapRepository.findFirstByUserIdOrderByGeneratedAtDesc(user.getId())
                .map(this::toDTO)
                .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_NOT_FOUND));
    }

    public List<DietRoadmapDTO> getHistory(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return dietRoadmapRepository.findAllByUserIdOrderByGeneratedAtDesc(user.getId())
                .stream().map(this::toDTO).toList();
    }

    public void delete(String username, String roadmapId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        dietRoadmapRepository.deleteByIdAndUserId(roadmapId, user.getId());
    }

    // ─── BMI Helpers ────────────────────────────────────────────────────────────

    private double calcBmi(double weight, double height) {
        double h = height / 100.0;
        return Math.round((weight / (h * h)) * 10.0) / 10.0;
    }

    private String classifyBmi(double bmi) {
        if (bmi < 18.5) return "UNDERWEIGHT";
        if (bmi < 23.0) return "NORMAL";
        if (bmi < 25.0) return "OVERWEIGHT";
        if (bmi < 30.0) return "OBESE_1";
        return "OBESE_2";
    }

    private String bmiCategoryLabel(String category) {
        return switch (category) {
            case "UNDERWEIGHT" -> "Gầy / Thiếu cân";
            case "NORMAL"      -> "Bình thường";
            case "OVERWEIGHT"  -> "Thừa cân";
            case "OBESE_1"     -> "Béo phì độ 1";
            default            -> "Béo phì độ 2";
        };
    }

    // ─── RAG Vector Search ──────────────────────────────────────────────────────

    private String getRagContext(List<String> diseases, String goal) {
        String query = buildRagQuery(diseases, goal);
        List<Double> embedding = getEmbedding(query);
        if (embedding == null || embedding.isEmpty()) return "";

        List<Document> pipeline = List.of(
            new Document("$vectorSearch",
                new Document("index", "vector_index")
                    .append("path", "embedding")
                    .append("queryVector", embedding)
                    .append("numCandidates", 50)
                    .append("limit", 4)
            ),
            new Document("$project", new Document("text", 1).append("_id", 0))
        );

        StringBuilder sb = new StringBuilder();
        try {
            mongoTemplate.getCollection("vector_store").aggregate(pipeline)
                    .forEach(doc -> sb.append("- ").append(doc.getString("text")).append("\n\n"));
        } catch (Exception e) {
            log.warn("RAG vector search failed: {}", e.getMessage());
        }
        return sb.toString().trim();
    }

    private String buildRagQuery(List<String> diseases, String goal) {
        if (diseases == null || diseases.isEmpty()) {
            return "chế độ dinh dưỡng " + (goal != null ? goal : "duy trì sức khỏe");
        }
        return "dinh dưỡng cho người bị " + String.join(", ", diseases);
    }

    private List<Double> getEmbedding(String text) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String safe = text.replace("\"", "\\\"").replace("\n", "\\n");
        String body = "{\"model\": \"models/gemini-embedding-001\", \"content\": {\"parts\": [{\"text\": \"" + safe + "\"}]}, \"outputDimensionality\": 768}";
        HttpEntity<String> req = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
            Map<String, Object> respBody = resp.getBody();
            if (respBody != null && respBody.containsKey("embedding")) {
                Map<String, Object> embNode = (Map<String, Object>) respBody.get("embedding");
                return (List<Double>) embNode.get("values");
            }
        } catch (Exception e) {
            log.warn("Embedding API failed: {}", e.getMessage());
        }
        return null;
    }

    // ─── Menu Context ────────────────────────────────────────────────────────────

    private String buildMenuContext() {
        List<Meal> meals = mealRepository.findAll();
        if (meals.size() > 15) {
            Collections.shuffle(meals);
            meals = meals.subList(0, 15);
        }
        StringBuilder sb = new StringBuilder();
        for (Meal m : meals) {
            sb.append("- ").append(m.getName());
            if (m.getNutrition() != null) {
                sb.append(" [Calo:").append(m.getNutrition().getCalories())
                  .append(", P:").append(m.getNutrition().getProtein()).append("g")
                  .append(", C:").append(m.getNutrition().getCarbs()).append("g")
                  .append(", F:").append(m.getNutrition().getFat()).append("g]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ─── Prompt Builder ──────────────────────────────────────────────────────────

    private String buildPrompt(UserMetrics m, double bmi, String bmiLabel,
                               String ragContext, String menuContext, String durationLabel) {
        int phaseCount = switch (durationLabel) {
            case "1_MONTH"   -> 2;
            case "3_MONTHS"  -> 3;
            case "6_MONTHS"  -> 4;
            case "12_MONTHS" -> 6;
            default -> 3;
        };

        String goalVi = switch (m.getGoal() != null ? m.getGoal() : "") {
            case "cut"      -> "Giảm mỡ / Giảm cân";
            case "bulk"     -> "Tăng cơ / Tăng cân";
            case "maintain" -> "Duy trì cân nặng";
            default         -> m.getGoal();
        };

        String diseasesText = (m.getDiseases() == null || m.getDiseases().isEmpty())
                ? "Không có bệnh lý đặc biệt"
                : String.join(", ", m.getDiseases());

        String durationVi = switch (durationLabel) {
            case "1_MONTH"   -> "1 tháng";
            case "3_MONTHS"  -> "3 tháng";
            case "6_MONTHS"  -> "6 tháng";
            case "12_MONTHS" -> "12 tháng";
            default -> durationLabel;
        };

        return """
Bạn là chuyên gia dinh dưỡng AI của MealYummy. Tạo lộ trình ăn uống %s cá nhân hóa.

--- THÔNG TIN NGƯỜI DÙNG ---
Tuổi: %d | Giới tính: %s
Cân nặng: %.1f kg | Chiều cao: %.1f cm
BMI: %.1f → %s
Mức hoạt động: %s
Mục tiêu: %s
Bệnh lý: %s

--- KIẾN THỨC Y KHOA (RAG) ---
%s

--- MENU MEALYUMMY ---
%s

--- YÊU CẦU ---
Tạo CHÍNH XÁC %d giai đoạn (phase). Trả về JSON hợp lệ KHÔNG có markdown, KHÔNG có ```json.
Schema bắt buộc:
{
  "overallGoal": "string",
  "summary": "string",
  "phases": [
    {
      "phaseNumber": 1,
      "phaseName": "string",
      "durationWeeks": "string",
      "startWeek": "string",
      "endWeek": "string",
      "targetGoal": "string",
      "targetCaloriesPerDay": 0,
      "targetProteinGram": 0,
      "targetCarbsGram": 0,
      "targetFatGram": 0,
      "allowedFoods": ["string"],
      "avoidedFoods": ["string"],
      "recommendedMealNames": ["string"],
      "aiAdvice": "string",
      "breakfastTemplate": "string",
      "lunchTemplate": "string",
      "dinnerTemplate": "string",
      "snackTemplate": "string"
    }
  ]
}
""".formatted(
                durationVi, m.getAge(), m.getGender() != null ? m.getGender() : "N/A",
                m.getWeight(), m.getHeight(), bmi, bmiLabel,
                m.getActivity() != null ? m.getActivity() : "moderate",
                goalVi, diseasesText,
                ragContext.isEmpty() ? "Không có context" : ragContext,
                menuContext.isEmpty() ? "Không có menu" : menuContext,
                phaseCount
        );
    }

    // ─── Gemini API Call ─────────────────────────────────────────────────────────

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        String body = "{\"contents\": [{\"role\": \"user\", \"parts\": [{\"text\": \"" + safePrompt + "\"}]}]}";

        HttpEntity<String> req = new HttpEntity<>(body, headers);
        
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
                Map<String, Object> respBody = resp.getBody();
                if (respBody != null && respBody.containsKey("candidates")) {
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) respBody.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                        return (String) parts.get(0).get("text");
                    }
                }
            } catch (Exception e) {
                log.error("Gemini API error (Attempt {}/{}): {}", i + 1, maxRetries, e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(2000); // Đợi 2 giây rồi thử lại
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
        
        throw new AppException(ErrorCode.AI_GENERATION_FAILED);
    }

    // ─── Parse & Save ────────────────────────────────────────────────────────────

    private DietRoadmap parseAndSave(String aiJson, String userId, double bmi,
                                     String bmiCategory, String bmiLabel,
                                     UserMetrics metrics, String durationLabel) {
        // Clean potential markdown wrappers
        String cleaned = aiJson.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            List<RoadmapPhase> phases = new ArrayList<>();
            JsonNode phasesNode = root.get("phases");
            if (phasesNode != null && phasesNode.isArray()) {
                for (JsonNode p : phasesNode) {
                    RoadmapPhase phase = new RoadmapPhase();
                    phase.setPhaseNumber(p.path("phaseNumber").asInt());
                    phase.setPhaseName(p.path("phaseName").asText());
                    phase.setDurationWeeks(p.path("durationWeeks").asText());
                    phase.setStartWeek(p.path("startWeek").asText());
                    phase.setEndWeek(p.path("endWeek").asText());
                    phase.setTargetGoal(p.path("targetGoal").asText());
                    phase.setTargetCaloriesPerDay(p.path("targetCaloriesPerDay").asDouble());
                    phase.setTargetProteinGram(p.path("targetProteinGram").asDouble());
                    phase.setTargetCarbsGram(p.path("targetCarbsGram").asDouble());
                    phase.setTargetFatGram(p.path("targetFatGram").asDouble());
                    phase.setAllowedFoods(toStringList(p.get("allowedFoods")));
                    phase.setAvoidedFoods(toStringList(p.get("avoidedFoods")));
                    phase.setRecommendedMealNames(toStringList(p.get("recommendedMealNames")));
                    phase.setAiAdvice(p.path("aiAdvice").asText());
                    phase.setBreakfastTemplate(p.path("breakfastTemplate").asText());
                    phase.setLunchTemplate(p.path("lunchTemplate").asText());
                    phase.setDinnerTemplate(p.path("dinnerTemplate").asText());
                    phase.setSnackTemplate(p.path("snackTemplate").asText());
                    phases.add(phase);
                }
            }

            DietRoadmap roadmap = DietRoadmap.builder()
                    .userId(userId)
                    .durationLabel(durationLabel)
                    .bmi(bmi)
                    .bmiCategory(bmiCategory)
                    .bmiLabel(bmiLabel)
                    .diseases(metrics.getDiseases())
                    .overallGoal(root.path("overallGoal").asText())
                    .summary(root.path("summary").asText())
                    .phases(phases)
                    .status("ACTIVE")
                    .generatedAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            return dietRoadmapRepository.save(roadmap);

        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_GENERATION_FAILED);
        }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                list.add(item.asText());
            }
        }
        return list;
    }

    // ─── DTO Mapper ──────────────────────────────────────────────────────────────

    private DietRoadmapDTO toDTO(DietRoadmap r) {
        return DietRoadmapDTO.builder()
                .id(r.getId())
                .durationLabel(r.getDurationLabel())
                .bmi(r.getBmi())
                .bmiCategory(r.getBmiCategory())
                .bmiLabel(r.getBmiLabel())
                .diseases(r.getDiseases())
                .overallGoal(r.getOverallGoal())
                .summary(r.getSummary())
                .phases(r.getPhases())
                .status(r.getStatus())
                .generatedAt(r.getGeneratedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
