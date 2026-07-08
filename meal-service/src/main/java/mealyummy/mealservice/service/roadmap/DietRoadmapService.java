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
import mealyummy.mealservice.model.entity.MealPlanItem;
import mealyummy.mealservice.model.pojo.RoadmapPhase;
import mealyummy.mealservice.model.repository.DietRoadmapRepository;
import mealyummy.mealservice.model.repository.MealPlanRepository;
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

import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DietRoadmapService {

    private final UserRepository userRepository;
    private final UserMetricsRepository userMetricsRepository;
    private final MealRepository mealRepository;
    private final DietRoadmapRepository dietRoadmapRepository;
    private final MealPlanRepository mealPlanRepository;
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

        // 2. Chạy song song: RAG context và Menu context
        java.util.concurrent.CompletableFuture<String> ragFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> getRagContext(metrics.getDiseases(), metrics.getGoal()));
        java.util.concurrent.CompletableFuture<String> menuFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(this::buildMenuContext);

        String ragContext;
        String menuContext;
        try {
            ragContext = ragFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
            menuContext = menuFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Parallel fetch timeout, falling back to sequential: {}", e.getMessage());
            ragContext = getRagContext(metrics.getDiseases(), metrics.getGoal());
            menuContext = buildMenuContext();
        }

        // 3. Build prompt
        String prompt = buildPrompt(metrics, bmi, bmiLabel, ragContext, menuContext, request.getDurationLabel());

        // 4. Gọi Gemini AI
        String aiJson = callGemini(prompt);

        // 5. Parse JSON → entity
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

    public int syncToMealPlan(String username, String roadmapId, int days) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(today.plusDays(i).format(formatter));
        }
        return syncToMealPlanInternal(username, roadmapId, dates);
    }

    public int syncToMealPlanForDate(String username, String roadmapId, String date) {
        return syncToMealPlanInternal(username, roadmapId, List.of(date));
    }

    private int syncToMealPlanInternal(String username, String roadmapId, List<String> dates) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        DietRoadmap roadmap = dietRoadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_NOT_FOUND));

        if (!roadmap.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.ROADMAP_NOT_FOUND);
        }

        if (roadmap.getPhases() == null || roadmap.getPhases().isEmpty()) {
            throw new AppException(ErrorCode.ROADMAP_NOT_FOUND);
        }

        mealyummy.mealservice.model.entity.profile.UserMetrics metrics = userMetricsRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        List<String> diseases = (metrics != null && metrics.getDiseases() != null) ? metrics.getDiseases() : Collections.emptyList();
        List<String> blacklistedKeywords = buildDiseaseBlacklist(diseases);

        List<Meal> meals = mealRepository.findAll();
        // Lọc bỏ các món không có calo hoặc dữ liệu lỗi để đảm bảo frontend hiển thị đầy đủ
        meals = meals.stream().filter(m -> m.getNutrition() != null && m.getNutrition().getCalories() != null && m.getNutrition().getCalories() > 0).toList();
        if (meals.isEmpty()) return 0;

        // 1. LỌC TUYỆT ĐỐI THEO BỆNH LÝ (Bảo vệ sức khỏe người dùng)
        List<Meal> safeMeals = meals.stream().filter(m -> {
            String text = (m.getName() + " " + (m.getIngredients() != null ? m.getIngredients().toString() : "")).toLowerCase();
            return blacklistedKeywords.stream().noneMatch(bad -> text.contains(bad));
        }).toList();

        // Fallback cực đoan: Nếu bộ lọc quá gắt gao làm mất gần hết DB, phải nới lỏng để đảm bảo thuật toán chọn đủ 4 món khác nhau
        if (safeMeals.size() < 4) {
            safeMeals = new ArrayList<>(meals);
        }

        // Base TDEE Calculation (fallback if AI doesn't provide calories)
        double baseTdee = 2000.0;
        if (metrics != null) {
            double bmr = "male".equalsIgnoreCase(metrics.getGender()) 
                ? (10 * metrics.getWeight() + 6.25 * metrics.getHeight() - 5 * metrics.getAge() + 5)
                : (10 * metrics.getWeight() + 6.25 * metrics.getHeight() - 5 * metrics.getAge() - 161);
            
            double activityMult = 1.55; // moderate
            if ("sedentary".equalsIgnoreCase(metrics.getActivity())) activityMult = 1.2;
            else if ("light".equalsIgnoreCase(metrics.getActivity())) activityMult = 1.375;
            else if ("active".equalsIgnoreCase(metrics.getActivity())) activityMult = 1.725;
            else if ("very_active".equalsIgnoreCase(metrics.getActivity())) activityMult = 1.9;
            
            double tdee = bmr * activityMult;
            double goalAdj = 0;
            if ("cut".equalsIgnoreCase(metrics.getGoal())) goalAdj = -500;
            else if ("bulk".equalsIgnoreCase(metrics.getGoal())) goalAdj = 300;
            
            baseTdee = tdee + goalAdj;
            if (baseTdee < 1200) baseTdee = 1200;
        }

        // Xóa thực đơn tự động của những ngày chuẩn bị gen
        mealPlanRepository.deleteByUserIdAndDateIn(user.getId(), dates);

        Map<String, Double> mealPercentages = new HashMap<>();
        mealPercentages.put("breakfast", 0.25);
        mealPercentages.put("lunch", 0.35);
        mealPercentages.put("snack", 0.10);
        mealPercentages.put("dinner", 0.30);

        List<MealPlanItem> newItems = new ArrayList<>();
        String[] mealTypes = {"breakfast", "lunch", "snack", "dinner"};

        for (int i = 0; i < dates.size(); i++) {
            String d = dates.get(i);
            
            // 2. MAPPING THEO PHASE (Dựa trên tuần hiện tại)
            int currentWeek = (i / 7) + 1;
            RoadmapPhase currentPhase = roadmap.getPhases().get(0); // fallback
            
            for (RoadmapPhase p : roadmap.getPhases()) {
                try {
                    String sStr = p.getStartWeek() != null ? p.getStartWeek().replaceAll("[^0-9]", "") : "";
                    String eStr = p.getEndWeek() != null ? p.getEndWeek().replaceAll("[^0-9]", "") : "";
                    if (!sStr.isEmpty() && !eStr.isEmpty()) {
                        int start = Integer.parseInt(sStr);
                        int end = Integer.parseInt(eStr);
                        if (currentWeek >= start && currentWeek <= end) {
                            currentPhase = p;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Lấy lượng Calo từ AI, nếu AI trả về 0 thì dùng TDEE tính tay
            double targetCalories = (currentPhase.getTargetCaloriesPerDay() > 500)
                    ? currentPhase.getTargetCaloriesPerDay() 
                    : baseTdee;

            List<String> allowed = currentPhase.getAllowedFoods() != null ? currentPhase.getAllowedFoods() : Collections.emptyList();
            List<String> recommended = currentPhase.getRecommendedMealNames() != null ? currentPhase.getRecommendedMealNames() : Collections.emptyList();

            double accumulatedExpectedCal = 0;
            double accumulatedActualCal = 0;
            Set<String> chosenMealIdsThisDay = new HashSet<>();

            for (String t : mealTypes) {
                double expectedMealCal = targetCalories * mealPercentages.get(t);
                accumulatedExpectedCal += expectedMealCal;
                
                // Bù trừ Calo (Smart Calorie Compensation)
                double adjustedTargetCal = accumulatedExpectedCal - accumulatedActualCal;
                
                // Nếu là buổi phụ (snack) là tùy chọn, nếu lượng calo còn lại quá thấp thì có thể bỏ qua
                if (t.equals("snack") && adjustedTargetCal < 80) {
                    continue; 
                }
                if (adjustedTargetCal < 50) adjustedTargetCal = 50;

                // Phân loại bữa ăn (Strict Meal Type Filtering) trên tập món an toàn
                List<Meal> filteredPool = safeMeals.stream().filter(m -> isSuitableForMealType(m, t)).toList();
                if (filteredPool.isEmpty()) {
                    filteredPool = new ArrayList<>(safeMeals);
                }

                // Chống trùng lặp (Variety Enforcement - Ít nhất 3 món khác nhau trong ngày)
                List<Meal> finalPool = new ArrayList<>(filteredPool);
                finalPool.removeIf(m -> chosenMealIdsThisDay.contains(m.getId()));
                
                // Nếu không còn món thỏa mãn loại bữa ăn, nới lỏng loại bữa ăn nhưng vẫn cấm trùng
                if (finalPool.isEmpty()) {
                    finalPool = new ArrayList<>(safeMeals);
                    finalPool.removeIf(m -> chosenMealIdsThisDay.contains(m.getId()));
                }
                
                // Trực tiếp fallback về TẤT CẢ CÁC MÓN để cố gắng chống trùng tuyệt đối
                if (finalPool.isEmpty()) {
                    finalPool = new ArrayList<>(meals);
                    finalPool.removeIf(m -> chosenMealIdsThisDay.contains(m.getId()));
                }
                
                // Nếu DB thực sự dưới 4 món thì đành chịu trùng lặp
                if (finalPool.isEmpty()) {
                    finalPool = meals;
                }
                
                final double finalTargetCal = adjustedTargetCal;
                List<Meal> sortedPool = finalPool.stream().sorted(Comparator.comparingDouble(m -> {
                    double cal = (m.getNutrition() != null && m.getNutrition().getCalories() != null) ? m.getNutrition().getCalories() : 0.0;
                    double calDiff = Math.abs(cal - finalTargetCal);
                    
                    // 2. TÍNH ĐIỂM (Scoring) DỰA TRÊN ROADMAP VÀ TỪ KHÓA MÓN VIỆT NAM
                    String text = (m.getName() + " " + (m.getIngredients() != null ? m.getIngredients().toString() : "")).toLowerCase();
                    double score = 0;
                    
                    boolean matchRec = recommended.stream().anyMatch(r -> text.contains(r.toLowerCase()));
                    boolean matchAllow = allowed.stream().anyMatch(a -> text.contains(a.toLowerCase()));
                    if (matchRec) score += 200;
                    if (matchAllow) score += 100;
                    
                    // Ưu tiên MẠNH NHẤT các món ăn phù hợp với người Việt Nam để chắc chắn lên top
                    if (t.equals("breakfast") && (text.contains("phở") || text.contains("bún") || text.contains("bánh mì") || text.contains("cháo") || text.contains("xôi"))) score += 500;
                    if ((t.equals("lunch") || t.equals("dinner")) && (text.contains("cơm") || text.contains("kho") || text.contains("xào") || text.contains("thịt") || text.contains("cá"))) score += 500;
                    if (t.equals("snack") && (text.contains("sinh tố") || text.contains("trái cây") || text.contains("sữa chua") || text.contains("nước ép") || text.contains("salad") || (text.contains("canh") && !text.contains("bánh canh")))) score += 500;

                    // CẤM TUYỆT ĐỐI Canh, Salad, Sinh tố, Trái cây ở các bữa chính (Sáng, Trưa, Tối)
                    if (!t.equals("snack") && ((text.contains("canh") && !text.contains("bánh canh")) || text.contains("salad") || text.contains("sinh tố") || text.contains("trái cây") || text.contains("nước ép") || text.contains("sữa chua"))) {
                        score -= 10000;
                    }

                    // Score càng cao, trừ càng nhiều vào Diff => Ưu tiên món Việt Nam tuyệt đối
                    return calDiff - (score * 10); 
                })).toList();
                
                int limit = Math.min(3, sortedPool.size()); // Giảm độ random để bốc sát calo/score hơn
                Meal bestMeal = sortedPool.get(new Random().nextInt(limit));

                chosenMealIdsThisDay.add(bestMeal.getId());
                double actualCal = (bestMeal.getNutrition() != null && bestMeal.getNutrition().getCalories() != null) ? bestMeal.getNutrition().getCalories() : 0.0;
                accumulatedActualCal += actualCal;

                MealPlanItem item = MealPlanItem.builder()
                        .userId(user.getId())
                        .recipeId(bestMeal.getId())
                        .recipeName(bestMeal.getName())
                        .recipeImage(bestMeal.getImages() != null && !bestMeal.getImages().isEmpty() ? bestMeal.getImages().get(0).getUrl() : "")
                        .calories(bestMeal.getNutrition() != null ? bestMeal.getNutrition().getCalories() + "" : "0")
                        .cookTime("15 phút")
                        .date(d)
                        .mealType(t)
                        .isEaten(false)
                        .createdAt(Instant.now())
                        .build();
                newItems.add(item);
            }
        }

        mealPlanRepository.saveAll(newItems);
        return newItems.size();
    }

    private List<String> buildDiseaseBlacklist(List<String> diseases) {
        Set<String> bads = new HashSet<>();
        if (diseases == null || diseases.isEmpty()) return new ArrayList<>();

        for (String d : diseases) {
            switch (d) {
                case "DIABETES":
                    bads.addAll(Arrays.asList("ngọt", "đường", "sữa đặc", "bánh ngọt", "chè", "nước ngọt", "bánh kẹo"));
                    break;
                case "GOUT":
                    bads.addAll(Arrays.asList("nội tạng", "lòng", "gan", "bò", "hải sản", "tôm", "cua", "bia", "rượu"));
                    break;
                case "HYPERTENSION":
                    bads.addAll(Arrays.asList("muối", "chiên", "mặn", "dưa muối", "cà pháo", "đồ hộp", "mắm"));
                    break;
                case "DYSLIPIDEMIA":
                case "FATTY_LIVER":
                    bads.addAll(Arrays.asList("chiên", "dầu mỡ", "nội tạng", "mỡ", "da gà", "thịt mỡ", "xào nhiều dầu"));
                    break;
                case "CHRONIC_KIDNEY":
                case "KIDNEY_STONE":
                    bads.addAll(Arrays.asList("muối", "mặn", "mắm", "dưa muối", "cà pháo"));
                    break;
                case "GERD":
                    bads.addAll(Arrays.asList("cay", "chua", "tiêu", "ớt", "chanh", "giấm", "đồ chua"));
                    break;
                case "LACTOSE_INTOLERANT":
                    bads.addAll(Arrays.asList("sữa bò", "phô mai", "bơ", "sữa tươi", "lactose"));
                    break;
                case "HEMORRHOID":
                    bads.addAll(Arrays.asList("cay", "nóng", "ớt", "tiêu", "chiên"));
                    break;
            }
        }
        return new ArrayList<>(bads);
    }

    private boolean isSuitableForMealType(Meal meal, String mealType) {
        String text = (meal.getName() + " " + (meal.getDescription() != null ? meal.getDescription() : "")).toLowerCase();
        
        if (meal.getCategories() != null) {
            for (String c : meal.getCategories()) {
                if (c != null) text += " " + c.toLowerCase();
            }
        }
        if (meal.getTags() != null) {
            for (String t : meal.getTags()) {
                if (t != null) text += " " + t.toLowerCase();
            }
        }

        return switch (mealType) {
            case "breakfast" -> text.contains("sáng") || text.contains("breakfast") || text.contains("phở") || text.contains("bún") || text.contains("bánh mì") || text.contains("cháo") || text.contains("pancake") || text.contains("yến mạch") || text.contains("trứng") || text.contains("granola") || text.contains("xôi");
            case "lunch" -> text.contains("trưa") || text.contains("lunch") || text.contains("cơm") || text.contains("salad") || text.contains("bò") || text.contains("gà") || text.contains("cá") || text.contains("ức") || text.contains("thịt") || text.contains("canh") || text.contains("kho") || text.contains("xào");
            case "dinner" -> text.contains("tối") || text.contains("dinner") || text.contains("salad") || text.contains("canh") || text.contains("cá") || text.contains("gà") || text.contains("rau") || text.contains("súp") || text.contains("ức") || text.contains("cơm");
            case "snack" -> text.contains("vặt") || text.contains("phụ") || text.contains("snack") || text.contains("sữa chua") || text.contains("sinh tố") || text.contains("trái cây") || text.contains("hạt") || text.contains("whey") || text.contains("smoothie") || text.contains("chuối") || text.contains("nước ép");
            default -> true;
        };
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
        if (meals.size() > 500) {
            Collections.shuffle(meals);
            meals = meals.subList(0, 500);
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
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate cursor = today;

            if (phasesNode != null && phasesNode.isArray()) {
                int phaseIndex = 0;
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

                    // ── Tính startDate / endDate thực tế ──────────────────────
                    int weeks = parseDurationWeeks(phase.getDurationWeeks());
                    phase.setStartDate(cursor.format(fmt));
                    phase.setEndDate(cursor.plusWeeks(weeks).minusDays(1).format(fmt));
                    cursor = cursor.plusWeeks(weeks);

                    // ── Chỉ Phase 1 là ACTIVE, các phase còn lại LOCKED ───────
                    phase.setPhaseStatus(phaseIndex == 0 ? "ACTIVE" : "LOCKED");

                    // ── Target cân nặng (ước tính từ goal + thời gian) ────────
                    double weightTarget = estimateTargetWeight(metrics, weeks);
                    phase.setTargetWeight(weightTarget);

                    phases.add(phase);
                    phaseIndex++;
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

    /** Phân tích số tuần từ chuỗi dạng "2 tuần", "2-3 weeks", v.v. */
    private int parseDurationWeeks(String durationWeeks) {
        if (durationWeeks == null || durationWeeks.isBlank()) return 2;
        try {
            // Lấy số đầu tiên xuất hiện trong chuỗi
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(durationWeeks);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return 2;
    }

    /** Ước tính cân nặng mục tiêu sau X tuần dựa vào goal của người dùng */
    private double estimateTargetWeight(UserMetrics metrics, int weeks) {
        double current = metrics.getWeight();
        String goal = metrics.getGoal() != null ? metrics.getGoal() : "maintain";
        double weeklyChange = switch (goal) {
            case "cut"  -> -0.5;  // giảm 0.5kg/tuần
            case "bulk" -> +0.3;  // tăng 0.3kg/tuần
            default     -> 0.0;
        };
        return Math.round((current + weeklyChange * weeks) * 10.0) / 10.0;
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

    // ─── Phase Unlock ────────────────────────────────────────────────────────────

    /**
     * Mở khóa phase tiếp theo sau khi người dùng cập nhật chỉ số.
     * - Nếu đạt target cân nặng  → chỉ chuyển trạng thái, giữ nguyên phase đã sinh.
     * - Nếu chưa đạt target      → gọi AI tính lại phase kế tiếp rồi thay thế.
     */
    public DietRoadmapDTO unlockNextPhase(String username, String roadmapId, int currentPhaseNumber) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        DietRoadmap roadmap = dietRoadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_NOT_FOUND));

        if (!roadmap.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.ROADMAP_NOT_FOUND);
        }

        List<RoadmapPhase> phases = roadmap.getPhases();
        if (phases == null || phases.isEmpty()) throw new AppException(ErrorCode.ROADMAP_NOT_FOUND);

        // Tìm phase hiện tại
        RoadmapPhase currentPhase = phases.stream()
                .filter(p -> p.getPhaseNumber() == currentPhaseNumber)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.ROADMAP_NOT_FOUND));

        // Lấy metrics mới nhất của user
        UserMetrics latestMetrics = userMetricsRepository
                .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_METRICS_NOT_FOUND));

        // ── BẮT BUỘC: user phải cập nhật hồ sơ trong vòng 48 giờ trước khi mở khóa ──
        // Đảm bảo người dùng luôn chủ động nhập lại chỉ số trước khi AI đánh giá
        Instant cutoff = Instant.now().minus(48, java.time.temporal.ChronoUnit.HOURS);
        if (latestMetrics.getCreatedAt() == null || latestMetrics.getCreatedAt().isBefore(cutoff)) {
            throw new AppException(ErrorCode.METRICS_NOT_UPDATED);
        }

        // So sánh cân nặng hiện tại vs target phase
        double actualWeight = latestMetrics.getWeight();
        double targetWeight = currentPhase.getTargetWeight();
        boolean goalCut = "cut".equalsIgnoreCase(latestMetrics.getGoal());
        boolean goalBulk = "bulk".equalsIgnoreCase(latestMetrics.getGoal());

        // Đạt target: giảm đủ (cut) hoặc tăng đủ (bulk) hoặc duy trì (maintain)
        boolean achieved;
        if (goalCut)       achieved = actualWeight <= targetWeight + 0.5;  // cho phép sai số 0.5kg
        else if (goalBulk) achieved = actualWeight >= targetWeight - 0.5;
        else               achieved = Math.abs(actualWeight - targetWeight) <= 1.0;

        log.info("Phase unlock — user: {}, phaseNumber: {}, actualWeight: {}, targetWeight: {}, achieved: {}",
                username, currentPhaseNumber, actualWeight, targetWeight, achieved);

        // Đánh dấu phase hiện tại là COMPLETED
        currentPhase.setPhaseStatus("COMPLETED");

        // Tìm phase tiếp theo
        int nextPhaseNumber = currentPhaseNumber + 1;
        RoadmapPhase nextPhase = phases.stream()
                .filter(p -> p.getPhaseNumber() == nextPhaseNumber)
                .findFirst()
                .orElse(null);

        if (nextPhase == null) {
            // Không còn phase tiếp theo → roadmap hoàn thành
            roadmap.setStatus("COMPLETED");
            roadmap.setUpdatedAt(Instant.now());
            return toDTO(dietRoadmapRepository.save(roadmap));
        }

        if (achieved) {
            // Đạt: chỉ mở khóa phase tiếp theo
            nextPhase.setPhaseStatus("ACTIVE");
            log.info("Phase {} achieved — unlocking phase {} as-is", currentPhaseNumber, nextPhaseNumber);
        } else {
            // Chưa đạt: AI tính toán lại phase tiếp theo
            log.info("Phase {} not achieved — recalculating phase {}", currentPhaseNumber, nextPhaseNumber);
            RoadmapPhase recalculated = recalculatePhase(latestMetrics, nextPhase, roadmap);
            // Thay thế trong list
            int idx = phases.indexOf(nextPhase);
            phases.set(idx, recalculated);
            nextPhase = recalculated;
            nextPhase.setPhaseStatus("ACTIVE");
        }

        roadmap.setUpdatedAt(Instant.now());
        return toDTO(dietRoadmapRepository.save(roadmap));
    }

    /** Gọi Gemini AI tính lại một phase dựa trên chỉ số hiện tại của user */
    private RoadmapPhase recalculatePhase(UserMetrics metrics, RoadmapPhase oldPhase, DietRoadmap roadmap) {
        String ragContext = getRagContext(metrics.getDiseases(), metrics.getGoal());
        String menuContext = buildMenuContext();

        String prompt = """
                Bạn là chuyên gia dinh dưỡng AI của MealYummy.
                Người dùng chưa đạt mục tiêu phase trước trong lộ trình dinh dưỡng.
                Hãy tính toán lại và điều chỉnh phase tiếp theo cho phù hợp hơn.

                --- THÔNG TIN NGƯỜI DÙNG HIỆN TẠI ---
                Cân nặng hiện tại: %.1f kg | Chiều cao: %.1f cm
                Tuổi: %d | Giới tính: %s | Mục tiêu: %s
                Bệnh lý: %s

                --- PHASE CẦN ĐIỀU CHỈNH ---
                Phase số: %d — %s
                Thời gian: %s
                Mục tiêu cũ: %s

                --- KIẾN THỨC Y KHOA ---
                %s

                --- MENU MEALYUMMY ---
                %s

                Trả về JSON hợp lệ KHÔNG có markdown cho DUY NHẤT 1 phase. Schema:
                {
                  "phaseNumber": %d,
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
                """.formatted(
                metrics.getWeight(), metrics.getHeight(), metrics.getAge(),
                metrics.getGender(), metrics.getGoal(),
                metrics.getDiseases() != null ? String.join(", ", metrics.getDiseases()) : "Không có",
                oldPhase.getPhaseNumber(), oldPhase.getPhaseName(),
                oldPhase.getDurationWeeks(), oldPhase.getTargetGoal(),
                ragContext.isEmpty() ? "Không có" : ragContext,
                menuContext.isEmpty() ? "Không có" : menuContext,
                oldPhase.getPhaseNumber()
        );

        String aiJson = callGemini(prompt);
        String cleaned = aiJson.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-z]*\\n?", "").replaceAll("```$", "").trim();
        }

        try {
            JsonNode node = objectMapper.readTree(cleaned);
            RoadmapPhase newPhase = new RoadmapPhase();
            newPhase.setPhaseNumber(node.path("phaseNumber").asInt(oldPhase.getPhaseNumber()));
            newPhase.setPhaseName(node.path("phaseName").asText());
            newPhase.setDurationWeeks(node.path("durationWeeks").asText(oldPhase.getDurationWeeks()));
            newPhase.setStartWeek(node.path("startWeek").asText(oldPhase.getStartWeek()));
            newPhase.setEndWeek(node.path("endWeek").asText(oldPhase.getEndWeek()));
            newPhase.setTargetGoal(node.path("targetGoal").asText());
            newPhase.setTargetCaloriesPerDay(node.path("targetCaloriesPerDay").asDouble());
            newPhase.setTargetProteinGram(node.path("targetProteinGram").asDouble());
            newPhase.setTargetCarbsGram(node.path("targetCarbsGram").asDouble());
            newPhase.setTargetFatGram(node.path("targetFatGram").asDouble());
            newPhase.setAllowedFoods(toStringList(node.get("allowedFoods")));
            newPhase.setAvoidedFoods(toStringList(node.get("avoidedFoods")));
            newPhase.setRecommendedMealNames(toStringList(node.get("recommendedMealNames")));
            newPhase.setAiAdvice(node.path("aiAdvice").asText());
            newPhase.setBreakfastTemplate(node.path("breakfastTemplate").asText());
            newPhase.setLunchTemplate(node.path("lunchTemplate").asText());
            newPhase.setDinnerTemplate(node.path("dinnerTemplate").asText());
            newPhase.setSnackTemplate(node.path("snackTemplate").asText());
            // Giữ nguyên startDate/endDate của phase cũ
            newPhase.setStartDate(oldPhase.getStartDate());
            newPhase.setEndDate(oldPhase.getEndDate());
            newPhase.setTargetWeight(estimateTargetWeight(metrics, parseDurationWeeks(newPhase.getDurationWeeks())));
            return newPhase;
        } catch (Exception e) {
            log.error("Failed to parse recalculated phase: {}", e.getMessage());
            // Fallback: trả về phase cũ
            return oldPhase;
        }
    }
}
