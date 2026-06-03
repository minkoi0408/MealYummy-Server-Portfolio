# 🗓️ DIET ROADMAP – Tài Liệu Kỹ Thuật Backend

> **Package:** `mealyummy-server/meal-service`
> **Phiên bản:** v1.0
> **Cập nhật:** 2026-05-27
> **Tác giả:** MealYummy Dev Team

---

## 📌 1. Tổng Quan Tính Năng

**Diet Roadmap** (Lộ Trình Thực Đơn Thông Minh) là tính năng cho phép hệ thống **tự động sinh lộ trình ăn uống dài hạn cá nhân hóa** dựa trên:

- 📊 **Chỉ số cơ thể**: BMI, cân nặng, chiều cao, tuổi, giới tính, % mỡ, khối cơ
- 🏃 **Mức độ hoạt động**: Ít vận động → Vận động nặng
- 🎯 **Mục tiêu**: Giảm cân / Tăng cơ / Duy trì / Phục hồi sức khỏe
- 🏥 **Bệnh lý**: Tiểu đường, Gout, Cao huyết áp, Thận, Gan, v.v.

Lộ trình được chia thành nhiều **giai đoạn (Phase)** với macro targets, danh sách thực phẩm nên ăn/cần tránh và lời khuyên AI chuyên sâu. Hỗ trợ **1 tháng → 3 tháng → 6 tháng → 12 tháng**.

---

## 🏗️ 2. Kiến Trúc & Luồng Xử Lý

```
Client Request
     │
     ▼
DietRoadmapController
     │  POST /api/v1/diet-roadmap/generate
     ▼
DietRoadmapService
     ├── 1. Lấy UserMetrics từ DB (BMI, diseases, goal...)
     ├── 2. Tính BMI → Phân loại BMI category
     ├── 3. Lấy RAG context từ VectorStore (top-3 chunks)
     ├── 4. Lấy danh sách món ăn từ MealRepository (random 15)
     ├── 5. Build Prompt cho Gemini 2.5 Flash
     ├── 6. Gọi Gemini API → nhận JSON có cấu trúc
     ├── 7. Parse JSON → DietRoadmap entity
     └── 8. Lưu vào MongoDB collection "diet_roadmaps"
     │
     ▼
DietRoadmapRepository (MongoDB)
     │
     ▼
Response: DietRoadmapDTO → Client
```

---

## 🗃️ 3. Data Schema

### 3.1 Entity: `DietRoadmap` → Collection `diet_roadmaps`

```java
@Document(collection = "diet_roadmaps")
public class DietRoadmap {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String durationLabel;   // "1_MONTH" | "3_MONTHS" | "6_MONTHS" | "12_MONTHS"
    private double bmi;             // Ví dụ: 27.3
    private String bmiCategory;     // "UNDERWEIGHT" | "NORMAL" | "OVERWEIGHT" | "OBESE"
    private String bmiLabel;        // "Thừa cân" (tiếng Việt để hiển thị UI)
    private List<String> diseases;  // ["DIABETES", "HYPERTENSION"]
    private String overallGoal;     // "Giảm cân và kiểm soát đường huyết"
    private String summary;         // Tóm tắt lộ trình do AI viết
    private List<RoadmapPhase> phases;
    private String status;          // "ACTIVE" | "COMPLETED" | "PAUSED"

    private Instant generatedAt;
    private Instant updatedAt;
}
```

### 3.2 POJO: `RoadmapPhase` (nhúng trong DietRoadmap)

```java
public class RoadmapPhase {
    private int phaseNumber;           // 1, 2, 3, 4...
    private String phaseName;          // "Khởi Động", "Tăng Tốc", "Duy Trì"...
    private String durationWeeks;      // "4 tuần"
    private String startWeek;          // "Tuần 1"
    private String endWeek;            // "Tuần 4"
    private String targetGoal;         // Mục tiêu cụ thể của giai đoạn
    private double targetCaloriesPerDay;
    private double targetProteinGram;
    private double targetCarbsGram;
    private double targetFatGram;
    private List<String> allowedFoods;       // Thực phẩm nên ăn
    private List<String> avoidedFoods;       // Thực phẩm cần tránh
    private List<String> recommendedMealIds; // ID món ăn trong DB
    private List<String> recommendedMealNames;
    private String aiAdvice;                 // Lời khuyên tổng quan từ AI
    private String breakfastTemplate;        // Thực đơn sáng điển hình
    private String lunchTemplate;            // Thực đơn trưa điển hình
    private String dinnerTemplate;           // Thực đơn tối điển hình
    private String snackTemplate;            // Bữa phụ điển hình
}
```

### 3.3 Thay đổi `UserMetrics` (mở rộng)

```java
// Thêm field mới vào entity đã có:
private List<String> diseases; // ["DIABETES", "GOUT", "HYPERTENSION"]
```

---

## 🔌 4. API Endpoints

### 4.1 Tạo Lộ Trình Mới

```
POST /api/v1/diet-roadmap/generate
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

Request Body:
{
  "durationLabel": "3_MONTHS"  // "1_MONTH" | "3_MONTHS" | "6_MONTHS" | "12_MONTHS"
}

Response 200:
{
  "code": 200,
  "message": "Tạo lộ trình thành công",
  "result": {
    "id": "abc123",
    "durationLabel": "3_MONTHS",
    "bmi": 27.3,
    "bmiCategory": "OVERWEIGHT",
    "bmiLabel": "Thừa cân",
    "diseases": ["HYPERTENSION"],
    "overallGoal": "Giảm 5kg trong 3 tháng và ổn định huyết áp",
    "summary": "...",
    "phases": [
      {
        "phaseNumber": 1,
        "phaseName": "Giai Đoạn Khởi Động",
        "durationWeeks": "4 tuần",
        "startWeek": "Tuần 1",
        "endWeek": "Tuần 4",
        "targetGoal": "Làm quen chế độ ăn nhạt, giảm 1.5kg",
        "targetCaloriesPerDay": 1800,
        "targetProteinGram": 120,
        "targetCarbsGram": 180,
        "targetFatGram": 60,
        "allowedFoods": ["Gạo lứt", "Ức gà luộc", "Rau cải", "Dưa leo"],
        "avoidedFoods": ["Mì tôm", "Xúc xích", "Nước mắm nhiều", "Thức ăn chiên"],
        "aiAdvice": "Ở giai đoạn này, ưu tiên hàng đầu là...",
        "breakfastTemplate": "Yến mạch + 2 lòng trắng trứng + chuối",
        "lunchTemplate": "Cơm gạo lứt 1 chén + ức gà 150g + rau luộc",
        "dinnerTemplate": "Cá hấp 150g + súp lơ xào tỏi + 1 chén cháo gạo lứt",
        "snackTemplate": "Táo 1 quả hoặc thanh long",
        "recommendedMealNames": ["Cơm Gà Hấp Gừng", "Salad Rau Trộn"]
      }
      // ... more phases
    ],
    "status": "ACTIVE",
    "generatedAt": "2026-05-27T04:20:00Z"
  }
}
```

### 4.2 Lấy Lộ Trình Hiện Tại

```
GET /api/v1/diet-roadmap
Authorization: Bearer {JWT_TOKEN}

Response 200: { DietRoadmapDTO }
Response 404: { "message": "Bạn chưa có lộ trình nào. Hãy tạo lộ trình đầu tiên!" }
```

### 4.3 Lịch Sử Lộ Trình

```
GET /api/v1/diet-roadmap/history
Authorization: Bearer {JWT_TOKEN}

Response 200:
{
  "result": [
    { "id": "abc123", "durationLabel": "3_MONTHS", "generatedAt": "...", "status": "COMPLETED" },
    { "id": "def456", "durationLabel": "1_MONTH",  "generatedAt": "...", "status": "COMPLETED" }
  ]
}
```

### 4.4 Xóa Lộ Trình

```
DELETE /api/v1/diet-roadmap/{id}
Authorization: Bearer {JWT_TOKEN}

Response 200: { "message": "Đã xóa lộ trình thành công" }
```

---

## 🤖 5. AI Prompt Strategy

### 5.1 Cấu trúc Prompt gửi Gemini

```
SYSTEM INSTRUCTION:
Bạn là chuyên gia dinh dưỡng và bác sĩ AI của MealYummy. 
Nhiệm vụ: Tạo lộ trình ăn uống [DURATION] cá nhân hóa dựa trên thông tin người dùng.
BẮT BUỘC trả về JSON hợp lệ theo đúng schema được cung cấp.

--- THÔNG TIN NGƯỜI DÙNG ---
- Tuổi: {age} | Giới tính: {gender}
- Cân nặng: {weight}kg | Chiều cao: {height}cm
- BMI: {bmi} → Phân loại: {bmiLabel}
- % Mỡ cơ thể: {bodyFat}% | Khối cơ: {muscleMass}kg
- Mức hoạt động: {activity}
- Mục tiêu: {goal}
- Bệnh lý hiện tại: {diseases_list}

--- KIẾN THỨC Y KHOA (RAG) ---
{rag_context_top3}

--- MENU NHÀ HÀNG MEALYUMMY ---
{meal_list_with_nutrition}

--- YÊU CẦU OUTPUT (JSON SCHEMA) ---
{
  "overallGoal": "...",
  "summary": "...",
  "phases": [
    {
      "phaseNumber": 1,
      "phaseName": "...",
      "durationWeeks": "...",
      "startWeek": "...",
      "endWeek": "...",
      "targetGoal": "...",
      "targetCaloriesPerDay": 0,
      "targetProteinGram": 0,
      "targetCarbsGram": 0,
      "targetFatGram": 0,
      "allowedFoods": ["..."],
      "avoidedFoods": ["..."],
      "aiAdvice": "...",
      "breakfastTemplate": "...",
      "lunchTemplate": "...",
      "dinnerTemplate": "...",
      "snackTemplate": "...",
      "recommendedMealNames": ["..."]
    }
  ]
}
```

### 5.2 Số lượng Phase theo Duration

| Duration | Số Phase | Mỗi Phase |
|----------|----------|-----------|
| 1_MONTH  | 2 phases | ~2 tuần/phase |
| 3_MONTHS | 3 phases | ~4 tuần/phase |
| 6_MONTHS | 4 phases | ~6 tuần/phase |
| 12_MONTHS | 6 phases | ~8 tuần/phase |

---

## 🏥 6. Danh Sách Bệnh Lý Hỗ Trợ

Tương ứng với kiến thức y khoa trong `medical_knowledge.txt`:

| Mã Code | Tên Hiển Thị | Mục RAG |
|---------|-------------|---------|
| `DIABETES` | Tiểu đường (Đái tháo đường) | Mục 1 |
| `GOUT` | Gout (Gút) | Mục 2 |
| `HYPERTENSION` | Cao huyết áp | Mục 3 |
| `DYSLIPIDEMIA` | Mỡ máu cao (Rối loạn lipid) | Mục 7 |
| `CHRONIC_KIDNEY` | Bệnh thận mạn tính | Mục 8 |
| `FATTY_LIVER` | Gan nhiễm mỡ / Viêm gan | Mục 9 |
| `KIDNEY_STONE` | Sỏi thận (Canxi Oxalate) | Mục 10 |
| `GERD` | Bệnh dạ dày / Trào ngược | Mục 6 |
| `IBS` | Hội chứng ruột kích thích | Mục 11 |
| `HEMORRHOID` | Bệnh trĩ / Táo bón mãn tính | Mục 12 |
| `ARTHRITIS` | Viêm khớp dạng thấp | Mục 13 |
| `THYROID` | Bệnh tuyến giáp (Suy/Cường) | Mục 14 |
| `GYM_BULK` | Đang tập Gym – Tăng cơ | Mục 4 |
| `GYM_CUT` | Đang tập Gym – Giảm mỡ | Mục 4 |
| `LACTOSE_INTOLERANT` | Bất dung nạp Lactose | Mục 16 |
| `FEVER_INFECTION` | Đang sốt / Nhiễm trùng | Mục 15 |
| `ACNE_HORMONAL` | Mụn nội tiết do chế độ ăn | Mục 18 |
| `MUSCLE_RECOVERY` | Đang phục hồi cơ / Chấn thương | Mục 20 |
| `HYPOGLYCEMIA` | Hạ đường huyết khi tập | Mục 17 |
| `DIGESTIVE_OVERLOAD` | Rối loạn tiêu hóa do nạp protein | Mục 19 |

---

## 📐 7. Logic Tính BMI & Phân Loại

```java
// Công thức: BMI = weight(kg) / (height(m))²
double bmi = weight / ((height / 100.0) * (height / 100.0));

// Phân loại theo chuẩn Châu Á (WHO 2000)
String category;
String label;
if (bmi < 18.5) {
    category = "UNDERWEIGHT"; label = "Gầy / Thiếu cân";
} else if (bmi < 23.0) {
    category = "NORMAL";      label = "Bình thường";
} else if (bmi < 25.0) {
    category = "OVERWEIGHT";  label = "Thừa cân";
} else if (bmi < 30.0) {
    category = "OBESE_1";     label = "Béo phì độ 1";
} else {
    category = "OBESE_2";     label = "Béo phì độ 2";
}
```

> **Lưu ý**: Dùng chuẩn Châu Á (ngưỡng 23 thay vì 25) vì phù hợp với người Việt Nam hơn.

---

## 📦 8. Cấu Trúc Package Mới

```
src/main/java/mealyummy/mealservice/
│
├── model/
│   ├── entity/
│   │   └── DietRoadmap.java          ← [NEW] MongoDB entity
│   ├── pojo/
│   │   └── RoadmapPhase.java         ← [NEW] Embedded POJO
│   └── repository/
│       └── DietRoadmapRepository.java ← [NEW] MongoDB repository
│
├── controller/
│   └── DietRoadmapController.java    ← [NEW] REST endpoints
│
└── service/
    └── roadmap/
        ├── DietRoadmapService.java    ← [NEW] Core business logic + AI
        └── dto/
            ├── DietRoadmapDTO.java    ← [NEW] Response DTO
            └── GenerateRoadmapRequest.java ← [NEW] Request DTO
```

---

## ⚙️ 9. Cấu Hình & Dependencies

Không cần thêm dependency mới. Tính năng tận dụng toàn bộ infrastructure đã có:

| Resource | Mục đích |
|----------|---------|
| MongoDB | Lưu `DietRoadmap` entity |
| Gemini 2.5 Flash API | Sinh lộ trình AI |
| VectorStore (MongoDB Atlas) | RAG lookup kiến thức y khoa |
| `MealRepository` | Lấy món ăn gợi ý |
| `UserMetricsRepository` | Lấy chỉ số cơ thể người dùng |
| JWT Security | Xác thực người dùng |

---

## 🚨 10. Lưu Ý Kỹ Thuật & Giới Hạn

### Giới hạn Context Gemini
- Lộ trình **12 tháng** KHÔNG sinh toàn bộ 365 ngày trong 1 lần gọi API
- Thay vào đó: Sinh **6 giai đoạn** với thực đơn tuần điển hình (weekly template)
- Mỗi phase chứa `breakfastTemplate`, `lunchTemplate`, `dinnerTemplate`, `snackTemplate` cho **1 ngày điển hình** trong giai đoạn đó

### Defensive JSON Parsing
```java
// Khi Gemini trả về text không đúng format JSON
// Service phải bắt JsonParseException và fallback:
try {
    roadmap = objectMapper.readValue(aiResponse, DietRoadmapDTO.class);
} catch (JsonParseException e) {
    // Retry với prompt ngắn hơn hoặc trả lỗi friendly
    throw new AppException(ErrorCode.AI_GENERATION_FAILED);
}
```

### Thời gian sinh lộ trình
- Dự kiến: **8–15 giây** (Gemini + VectorSearch + DB reads)
- Cần có loading indicator ở Frontend

---

## 🧪 11. Test Cases

```bash
# Tạo lộ trình 1 tháng – User thường, không bệnh
POST /api/v1/diet-roadmap/generate
{ "durationLabel": "1_MONTH" }
→ Expect: 2 phases, calo phù hợp với mục tiêu

# Tạo lộ trình 3 tháng – User có DIABETES + HYPERTENSION
POST /api/v1/diet-roadmap/generate
{ "durationLabel": "3_MONTHS" }
→ Expect: avoidedFoods chứa "cơm trắng", "muối", "xúc xích"

# User chưa nhập UserMetrics
POST /api/v1/diet-roadmap/generate
→ Expect: 400 Bad Request – "Vui lòng cập nhật chỉ số cơ thể trước"

# Lấy lộ trình khi chưa có
GET /api/v1/diet-roadmap
→ Expect: 404 Not Found
```

---

## 📅 12. Lộ Trình Phát Triển (Roadmap)

| Giai đoạn | Tính năng | Ưu tiên |
|----------|-----------|---------|
| **v1.0** | Sinh lộ trình AI, hiển thị timeline, checkbox bệnh lý | 🔴 Cao |
| **v1.1** | Export PDF lộ trình (tận dụng PDF service đã có) | 🟡 Trung bình |
| **v1.2** | Tự động đồng bộ phase hiện tại vào `meal_plan` calendar | 🟡 Trung bình |
| **v1.3** | Cập nhật tiến độ (chỉ số cơ thể thay đổi → AI điều chỉnh lộ trình) | 🟢 Thấp |
| **v2.0** | Vector Search tìm phase phù hợp (khi có >1000 lộ trình mẫu) | 🟢 Thấp |

---

*Tài liệu này được tạo ngày 27/05/2026. Mọi thay đổi cần cập nhật đồng thời vào file này.*
